/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.service.resource;

import static org.eclipse.keyple.core.service.resource.PluginsConfigurator.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.service.resource.spi.CardResourceProfileExtension;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.selection.spi.SmartCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager of a card profile.
 *
 * <p>It contains the profile configuration and associated card resources.
 *
 * @since 2.0.0
 */
final class CardProfileManagerAdapter {

  private static final Logger logger = LoggerFactory.getLogger(CardProfileManagerAdapter.class);

  /** The associated card profile. */
  private final CardResourceProfileConfigurator cardProfile;

  /** The global configuration of the card resource service. */
  private final CardResourceServiceConfiguratorAdapter globalConfiguration;

  /** The unique instance of the card resource service. */
  private final CardResourceServiceAdapter service;

  /** The ordered list of "regular" plugins to use. */
  private final List<Plugin> plugins;

  /** The ordered list of "pool" plugins to use. */
  private final List<PoolPlugin> poolPlugins;

  /** The current available card resources associated with "regular" plugins. */
  private final List<CardResourceAdapter> cardResources;

  /** The filter on the reader name if set. */
  private final Pattern readerNameRegexPattern;

  /**
   * Creates a new card profile manager using the provided card profile and initializes all
   * available card resources.
   *
   * @param cardProfile The associated card profile.
   * @param globalConfiguration The global configuration of the service.
   * @since 2.0.0
   */
  CardProfileManagerAdapter(
      CardResourceProfileConfigurator cardProfile,
      CardResourceServiceConfiguratorAdapter globalConfiguration) {

    this.cardProfile = cardProfile;
    this.globalConfiguration = globalConfiguration;
    service = CardResourceServiceAdapter.getInstance();
    plugins = new ArrayList<>(0);
    poolPlugins = new ArrayList<>(0);
    cardResources = new ArrayList<>();

    // Prepare filter on reader name if requested.
    if (cardProfile.getReaderNameRegex() != null) {
      readerNameRegexPattern = Pattern.compile(cardProfile.getReaderNameRegex());
    } else {
      readerNameRegexPattern = null;
    }

    // Initialize all available card resources.
    if (cardProfile.getPlugins().isEmpty()) {
      initializeCardResourcesUsingDefaultPlugins();
    } else {
      initializeCardResourcesUsingProfilePlugins();
    }
  }

  /** Initializes card resources using the plugins configured on the card profile. */
  private void initializeCardResourcesUsingProfilePlugins() {
    for (Plugin plugin : cardProfile.getPlugins()) {
      if (plugin instanceof PoolPlugin) {
        poolPlugins.add((PoolPlugin) plugin);
      } else {
        plugins.add(plugin);
        initializeCardResources(plugin);
      }
    }
  }

  /** Initializes card resources using the plugins configured on the card resource service. */
  private void initializeCardResourcesUsingDefaultPlugins() {
    poolPlugins.addAll(globalConfiguration.getPoolPlugins());
    for (Plugin plugin : globalConfiguration.getPlugins()) {
      plugins.add(plugin);
      initializeCardResources(plugin);
    }
  }

  /**
   * Initializes all available card resources by analysing all readers of the provided "regular"
   * plugin.
   *
   * @param plugin The "regular" plugin to analyse.
   */
  private void initializeCardResources(Plugin plugin) {
    for (CardReader reader : plugin.getReaders()) {
      ReaderManagerAdapter readerManager = service.getReaderManager(reader);
      initializeCardResource(readerManager);
    }
  }

  /**
   * Tries to initialize a card resource for the provided reader manager only if the reader is
   * accepted by the profile.
   *
   * <p>If the reader is accepted, then activates the provided reader manager if it is not already
   * activated.
   *
   * @param readerManager The reader manager to use.
   */
  private void initializeCardResource(ReaderManagerAdapter readerManager) {

    if (isReaderAccepted(readerManager.getReader())) {

      readerManager.activate();

      CardResourceAdapter cardResource =
          readerManager.matches(cardProfile.getCardResourceProfileExtension());

      // The returned card resource may already be present in the current list if the service starts
      // with an observable reader in which a card has been inserted.
      if (cardResource != null) {
        if (!cardResources.contains(cardResource)) {
          cardResources.add(cardResource);
          logger.info(
              "Add {} to profile [{}]",
              CardResourceServiceAdapter.getCardResourceInfo(cardResource),
              cardProfile.getProfileName());
        } else {
          logger.info(
              "{} already present in profile [{}]",
              CardResourceServiceAdapter.getCardResourceInfo(cardResource),
              cardProfile.getProfileName());
        }
      }
    }
  }

  /**
   * Checks if the provided reader is accepted using the filter on the name.
   *
   * @param reader The reader to check.
   * @return True if it is accepted.
   */
  private boolean isReaderAccepted(CardReader reader) {
    return readerNameRegexPattern == null
        || readerNameRegexPattern.matcher(reader.getName()).matches();
  }

  /**
   * Removes the provided card resource from the profile manager if it is present.
   *
   * @param cardResource The card resource to remove.
   * @since 2.0.0
   */
  void removeCardResource(CardResource cardResource) {
    boolean isRemoved = cardResources.remove(cardResource);
    if (isRemoved) {
      logger.info(
          "Remove {} from profile [{}]",
          CardResourceServiceAdapter.getCardResourceInfo(cardResource),
          cardProfile.getProfileName());
    }
  }

  /**
   * Invoked when a new reader is connected.<br>
   * If the associated plugin is referenced on the card profile, then tries to initialize a card
   * resource if the reader is accepted.
   *
   * @param readerManager The reader manager to use.
   * @since 2.0.0
   */
  void onReaderConnected(ReaderManagerAdapter readerManager) {
    if (cardProfile.getPlugins().isEmpty()) {
      initializeCardResource(readerManager);
    } else {
      for (Plugin profilePlugin : cardProfile.getPlugins()) {
        if (profilePlugin == readerManager.getPlugin()) {
          initializeCardResource(readerManager);
          break;
        }
      }
    }
  }

  /**
   * Invoked when a new card is inserted.<br>
   * The behaviour is the same as if a reader was connected.
   *
   * @param readerManager The reader manager to use.
   * @since 2.0.0
   */
  void onCardInserted(ReaderManagerAdapter readerManager) {
    onReaderConnected(readerManager);
  }

  /**
   * Tries to get a card resource and locks the associated reader.<br>
   * Applies the configured allocation strategy by looping, pausing, ordering resources.
   *
   * @return Null if there is no card resource available.
   * @since 2.0.0
   */
  CardResource getCardResource() {
    CardResource cardResource;
    long maxTime = System.currentTimeMillis() + globalConfiguration.getTimeoutMillis();
    do {
      if (plugins.isEmpty()) {
        cardResource = getPoolCardResource();
      } else {
        if (poolPlugins.isEmpty()) {
          cardResource = getRegularCardResource();
        } else {
          cardResource = getRegularOrPoolCardResource();
        }
      }
      pauseIfNeeded(cardResource);
    } while (cardResource == null
        && globalConfiguration.isBlockingAllocationMode()
        && System.currentTimeMillis() <= maxTime);
    return cardResource;
  }

  /**
   * Make a pause if the provided card resource is null and a blocking allocation mode is requested.
   *
   * @param cardResource The founded card resource or null if not found.
   */
  private void pauseIfNeeded(CardResource cardResource) {
    if (cardResource == null && globalConfiguration.isBlockingAllocationMode()) {
      try {
        Thread.sleep(globalConfiguration.getCycleDurationMillis());
      } catch (InterruptedException e) {
        logger.error("Unexpected sleep interruption", e);
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Tries to get a card resource searching in "regular" and "pool" plugins.
   *
   * @return Null if there is no card resource available.
   */
  private CardResource getRegularOrPoolCardResource() {
    CardResource cardResource;
    if (globalConfiguration.isUsePoolFirst()) {
      cardResource = getPoolCardResource();
      if (cardResource == null) {
        cardResource = getRegularCardResource();
      }
    } else {
      cardResource = getRegularCardResource();
      if (cardResource == null) {
        cardResource = getPoolCardResource();
      }
    }
    return cardResource;
  }

  /**
   * Tries to get a card resource searching in all "regular" plugins.
   *
   * <p>If a card resource is no more usable, then removes it from the service.
   *
   * @return Null if there is no card resource available.
   */
  private CardResource getRegularCardResource() {

    CardResource result = null;
    List<CardResource> unusableCardResources = new ArrayList<>(0);

    for (CardResource cardResource : cardResources) {
      CardReader reader = cardResource.getReader();
      synchronized (reader) {
        ReaderManagerAdapter readerManager = service.getReaderManager(reader);
        if (readerManager != null) {
          try {
            if (readerManager.lock(cardResource, cardProfile.getCardResourceProfileExtension())) {
              int cardResourceIndex = cardResources.indexOf(cardResource);
              updateCardResourcesOrder(cardResourceIndex);
              result = cardResource;
              break;
            }
          } catch (IllegalStateException e) {
            unusableCardResources.add(cardResource);
          }
        } else {
          unusableCardResources.add(cardResource);
        }
      }
    }

    // Remove unusable card resources identified.
    for (CardResource cardResource : unusableCardResources) {
      service.removeCardResource(cardResource);
    }

    return result;
  }

  /**
   * Updates the order of the created card resources according to the configured strategy.
   *
   * @param cardResourceIndex The current card resource index of the available card resource
   *     founded.
   */
  private void updateCardResourcesOrder(int cardResourceIndex) {
    if (globalConfiguration.getAllocationStrategy() == AllocationStrategy.CYCLIC) {
      Collections.rotate(cardResources, -cardResourceIndex - 1);
    } else if (globalConfiguration.getAllocationStrategy() == AllocationStrategy.RANDOM) {
      Collections.shuffle(cardResources);
    }
  }

  /**
   * Tries to get a card resource searching in all "pool" plugins.
   *
   * @return Null if there is no card resource available.
   */
  private CardResource getPoolCardResource() {
    CardResourceProfileExtension cardProfileExtension =
        cardProfile.getCardResourceProfileExtension();
    for (PoolPlugin poolPlugin : poolPlugins) {
      try {
        CardReader reader = poolPlugin.allocateReader(cardProfile.getReaderGroupReference());
        if (reader != null) {
          SmartCard selectedSmartCard = poolPlugin.getSelectedSmartCard(reader);
          SmartCard smartCard =
              selectedSmartCard != null
                  ? cardProfileExtension.matches(selectedSmartCard)
                  : cardProfileExtension.matches(
                      reader, SmartCardServiceProvider.getService().getReaderApiFactory());
          if (smartCard != null) {
            KeypleReaderExtension readerExtension =
                poolPlugin.getReaderExtension(KeypleReaderExtension.class, reader.getName());
            CardResource cardResource = new CardResourceAdapter(reader, readerExtension, smartCard);
            service.registerPoolCardResource(cardResource, poolPlugin);
            return cardResource;
          } else {
            releaseReaderSilently(poolPlugin, reader);
          }
        }
      } catch (KeyplePluginException e) {
        // Continue
      }
    }
    return null;
  }

  private static void releaseReaderSilently(PoolPlugin poolPlugin, CardReader reader) {
    try {
      poolPlugin.releaseReader(reader);
    } catch (Exception ignored) {
      // NOP
    }
  }
}
