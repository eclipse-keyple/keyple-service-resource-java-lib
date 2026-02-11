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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.SmartCardServiceProvider;
import org.eclipse.keyple.core.service.resource.spi.CardResourceProfileExtension;
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.selection.spi.IsoSmartCard;
import org.eclipse.keypop.reader.selection.spi.SmartCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager of a reader associated to a "regular" plugin.
 *
 * <p>It contains all associated created card resources and manages concurrent access to the
 * reader's card resources so that only one card resource can be used at a time.
 *
 * @since 2.0.0
 */
final class ReaderManagerAdapter {

  private static final Logger logger = LoggerFactory.getLogger(ReaderManagerAdapter.class);

  /** The associated reader */
  private final CardReader reader;

  /** The associated reader's extension */
  private final KeypleReaderExtension readerExtension;

  /** The associated plugin */
  private final Plugin plugin;

  /** Collection of all created card resources. */
  private final Set<CardResourceAdapter> cardResources;

  /** The reader configurator, not null if the monitoring is activated for the associated reader. */
  private final ReaderConfiguratorSpi readerConfiguratorSpi;

  /** The max usage duration of a card resource before it will be automatically release. */
  private final int usageTimeoutMillis;

  /**
   * Indicates the time after which the reader will be automatically unlocked if a new lock is
   * requested.
   */
  private long lockMaxTimeMillis;

  /** Current selected card resource. */
  private CardResource selectedCardResource;

  /** Indicates if a card resource is actually in use. */
  private volatile boolean isBusy;

  /** Indicates if the associated reader is accepted by at least one card profile manager. */
  private volatile boolean isActive;

  /**
   * Creates a new reader manager not active by default.
   *
   * @param reader The associated reader.
   * @param plugin The associated plugin.
   * @param readerConfiguratorSpi The reader configurator to use.
   * @param usageTimeoutMillis The max usage duration of a card resource before it will be
   *     automatically release (0 for infinite timeout).
   * @since 2.0.0
   */
  ReaderManagerAdapter(
      CardReader reader,
      Plugin plugin,
      ReaderConfiguratorSpi readerConfiguratorSpi,
      int usageTimeoutMillis) {
    this.reader = reader;
    readerExtension = plugin.getReaderExtension(KeypleReaderExtension.class, reader.getName());
    this.plugin = plugin;
    this.readerConfiguratorSpi = readerConfiguratorSpi;
    this.usageTimeoutMillis = usageTimeoutMillis;
    cardResources = Collections.newSetFromMap(new ConcurrentHashMap<>());
    selectedCardResource = null;
    isBusy = false;
    isActive = false;
  }

  /**
   * Gets the associated reader.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  CardReader getReader() {
    return reader;
  }

  /**
   * Gets the associated plugin.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  Plugin getPlugin() {
    return plugin;
  }

  /**
   * Gets a view of the current created card resources.
   *
   * @return An empty collection if there's no card resources.
   * @since 2.0.0
   */
  Set<CardResourceAdapter> getCardResources() {
    return cardResources;
  }

  /**
   * Indicates if the associated reader is accepted by at least one card profile manager.
   *
   * @return True if the reader manager is active.
   * @since 2.0.0
   */
  boolean isActive() {
    return isActive;
  }

  /**
   * Activates the reader manager and set up the reader if needed.
   *
   * @since 2.0.0
   */
  void activate() {
    if (!isActive) {
      readerConfiguratorSpi.setupReader(reader);
    }
    isActive = true;
  }

  /**
   * Gets a new or an existing card resource if the current inserted card matches with the provided
   * card resource profile extension.
   *
   * <p>If the card matches, then updates the current selected card resource.
   *
   * <p>In any case, invoking this method unlocks the reader due to the use of the card selection
   * manager by the extension during the match process.
   *
   * @param extension The card resource profile extension to use for matching.
   * @return Null if the inserted card does not match with the provided profile extension.
   * @since 2.0.0
   */
  CardResourceAdapter matches(CardResourceProfileExtension extension) {
    CardResourceAdapter cardResource = null;
    SmartCard smartCard =
        extension.matches(reader, SmartCardServiceProvider.getService().getReaderApiFactory());
    if (smartCard != null) {
      cardResource = getOrCreateCardResource(smartCard);
      selectedCardResource = cardResource;
    }
    unlock();
    return cardResource;
  }

  /**
   * Tries to lock the provided card resource if the reader is not busy.
   *
   * <p>If the provided card resource is not the current selected one, then tries to select it using
   * the provided card resource profile extension.
   *
   * @param cardResource The card resource to lock.
   * @param extension The card resource profile extension to use in case if a new selection is
   *     needed.
   * @return True if the card resource is locked.
   * @throws IllegalStateException If a new selection has been made and the current card does not
   *     match the provided profile extension or is not the same smart card than the provided one.
   * @since 2.0.0
   */
  boolean lock(CardResource cardResource, CardResourceProfileExtension extension) {
    if (isBusy) {
      if (usageTimeoutMillis == 0 || System.currentTimeMillis() < lockMaxTimeMillis) {
        return false;
      }
      logger.warn(
          "Reader automatically unlocked due to a usage timeout exceeded [reader={}, usageTimeoutMs={}]",
          reader.getName(),
          usageTimeoutMillis);
    }
    if (selectedCardResource != cardResource) {
      SmartCard smartCard =
          extension.matches(reader, SmartCardServiceProvider.getService().getReaderApiFactory());
      if (!areEquals(cardResource.getSmartCard(), smartCard)) {
        selectedCardResource = null;
        throw new IllegalStateException(
            "No card is inserted or its profile does not match the associated data");
      }
      selectedCardResource = cardResource;
    }
    lockMaxTimeMillis = System.currentTimeMillis() + usageTimeoutMillis;
    isBusy = true;
    return true;
  }

  /**
   * Free the reader.
   *
   * @since 2.0.0
   */
  void unlock() {
    isBusy = false;
  }

  /**
   * Removes the provided card resource.
   *
   * @param cardResource The card resource to remove.
   * @since 2.0.0
   */
  void removeCardResource(CardResource cardResource) {
    cardResources.remove(cardResource);
    if (selectedCardResource == cardResource) {
      selectedCardResource = null;
    }
  }

  /**
   * Gets an existing card resource having the same smart card than the provided one, or creates a
   * new one if not.
   *
   * @param smartCard The associated smart card.
   * @return A not null reference.
   */
  private CardResourceAdapter getOrCreateCardResource(SmartCard smartCard) {

    // Check if an identical card resource is already created.
    for (CardResourceAdapter cardResource : cardResources) {
      if (areEquals(cardResource.getSmartCard(), smartCard)) {
        return cardResource;
      }
    }

    // If none, then create a new one.
    CardResourceAdapter cardResource = new CardResourceAdapter(reader, readerExtension, smartCard);
    cardResources.add(cardResource);
    return cardResource;
  }

  /**
   * Checks if the provided Smart Cards are identical.
   *
   * @param s1 Smart Card 1
   * @param s2 Smart Card 2
   * @return True if they are identical.
   */
  private static boolean areEquals(SmartCard s1, SmartCard s2) {

    if (s1 == s2) {
      return true;
    }

    if (s1 == null || s2 == null) {
      return false;
    }

    boolean hasSamePowerOnData =
        (s1.getPowerOnData() == null && s2.getPowerOnData() == null)
            || (s1.getPowerOnData() != null && s1.getPowerOnData().equals(s2.getPowerOnData()));

    boolean hasSameFci;
    if (s1 instanceof IsoSmartCard && s2 instanceof IsoSmartCard) {
      hasSameFci =
          Arrays.equals(
              ((IsoSmartCard) s1).getSelectApplicationResponse(),
              ((IsoSmartCard) s2).getSelectApplicationResponse());
    } else {
      hasSameFci = true;
    }

    return hasSamePowerOnData && hasSameFci;
  }
}
