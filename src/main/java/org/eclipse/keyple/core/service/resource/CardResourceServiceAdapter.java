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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.keyple.core.service.*;
import org.eclipse.keyple.core.service.resource.spi.ReaderConfiguratorSpi;
import org.eclipse.keyple.core.service.spi.PluginObserverSpi;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.CardReaderEvent;
import org.eclipse.keypop.reader.ObservableCardReader;
import org.eclipse.keypop.reader.spi.CardReaderObserverSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link CardResourceService}.
 *
 * @since 2.0.0
 */
final class CardResourceServiceAdapter
    implements CardResourceService, PluginObserverSpi, CardReaderObserverSpi {

  private static final Logger logger = LoggerFactory.getLogger(CardResourceServiceAdapter.class);

  /** Singleton instance */
  private static final CardResourceServiceAdapter INSTANCE = new CardResourceServiceAdapter();

  /** Map an accepted reader of a "regular" plugin to a reader manager. */
  private final Map<CardReader, ReaderManagerAdapter> readerToReaderManagerMap =
      new ConcurrentHashMap<>();

  /** Map a configured card profile name to a card profile manager. */
  private final Map<String, CardProfileManagerAdapter> cardProfileNameToCardProfileManagerMap =
      new ConcurrentHashMap<>();

  /**
   * Map a card resource to a "pool plugin".<br>
   * A card resource associated to a "pool plugin" is only present in this map for the time of its
   * use and is not referenced by any card profile manager.
   */
  private final Map<CardResource, PoolPlugin> cardResourceToPoolPluginMap =
      new ConcurrentHashMap<>();

  /**
   * Map a "regular" plugin to its accepted observable readers referenced by at least one card
   * profile manager.<br>
   * This map is useful to observe only the accepted readers in case of a card monitoring request.
   */
  private final Map<Plugin, Set<ObservableCardReader>> pluginToObservableReadersMap =
      new ConcurrentHashMap<>();

  /** The current configuration. */
  private CardResourceServiceConfiguratorAdapter configurator;

  /** The current status of the card resource service. */
  private volatile boolean isStarted;

  /**
   * Gets the unique instance.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  static CardResourceServiceAdapter getInstance() {
    return INSTANCE;
  }

  /**
   * Gets a string representation of the provided card resource.
   *
   * @param cardResource The card resource.
   * @return Null if the provided card resource is null.
   * @since 2.0.0
   */
  static String getCardResourceInfo(CardResource cardResource) {
    return cardResource != null ? cardResource.toString() : null;
  }

  /**
   * Gets the reader manager associated to the provided reader.
   *
   * @param reader The associated reader.
   * @return Null if there is no reader manager associated.
   * @since 2.0.0
   */
  ReaderManagerAdapter getReaderManager(CardReader reader) {
    return readerToReaderManagerMap.get(reader);
  }

  /**
   * Associates a card resource to a "pool" plugin.
   *
   * @param cardResource The card resource to register.
   * @param poolPlugin The associated pool plugin.
   * @since 2.0.0
   */
  void registerPoolCardResource(CardResource cardResource, PoolPlugin poolPlugin) {
    cardResourceToPoolPluginMap.put(cardResource, poolPlugin);
  }

  /**
   * Configures the card resource service.
   *
   * <p>If service is started, then stops the service, applies the configuration and starts the
   * service.
   *
   * <p>If not, then only applies the configuration.
   *
   * @since 2.0.0
   */
  void configure(CardResourceServiceConfiguratorAdapter configurator) {
    logger.info("Applying new card resource service configuration");
    if (isStarted) {
      stop();
      this.configurator = configurator;
      start();
    } else {
      this.configurator = configurator;
    }
    logger.info("New card resource service configuration applied");
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public CardResourceServiceConfigurator getConfigurator() {
    return new CardResourceServiceConfiguratorAdapter();
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void start() {
    if (configurator == null) {
      throw new IllegalStateException("The card resource service is not configured");
    }
    if (isStarted) {
      stop();
    }
    logger.info("Starting card resource service");
    initializeReaderManagers();
    initializeCardProfileManagers();
    removeUnusedReaderManagers();
    startMonitoring();
    isStarted = true;
    logger.info("Card resource service started");
  }

  /** Initializes a reader manager for each reader of each configured "regular" plugin. */
  private void initializeReaderManagers() {
    for (Plugin plugin : configurator.getPlugins()) {
      for (CardReader reader : plugin.getReaders()) {
        registerReader(reader, plugin);
      }
    }
  }

  /**
   * Creates and registers a reader manager associated to the provided reader and its associated
   * plugin.<br>
   * If the provided reader is observable, then add it to the map of used observable readers.
   *
   * @param reader The reader to register.
   * @param plugin The associated plugin.
   * @return A not null reference.
   */
  private ReaderManagerAdapter registerReader(CardReader reader, Plugin plugin) {

    // Get the reader configurator if a monitoring is requested for this reader.
    ReaderConfiguratorSpi readerConfiguratorSpi = null;
    for (ConfiguredPlugin configuredPlugin : configurator.getConfiguredPlugins()) {
      if (configuredPlugin.getPlugin() == plugin) {
        readerConfiguratorSpi = configuredPlugin.getReaderConfiguratorSpi();
        break;
      }
    }

    ReaderManagerAdapter readerManager =
        new ReaderManagerAdapter(
            reader, plugin, readerConfiguratorSpi, configurator.getUsageTimeoutMillis());
    readerToReaderManagerMap.put(reader, readerManager);

    if (reader instanceof ObservableCardReader) {
      Set<ObservableCardReader> usedObservableReaders = pluginToObservableReadersMap.get(plugin);
      if (usedObservableReaders == null) {
        usedObservableReaders = Collections.newSetFromMap(new ConcurrentHashMap<>(1));
        pluginToObservableReadersMap.put(plugin, usedObservableReaders);
      }
      usedObservableReaders.add((ObservableCardReader) reader);
    }

    return readerManager;
  }

  /**
   * Creates and registers a card profile manager for each configured card profile and creates all
   * available card resources.
   */
  private void initializeCardProfileManagers() {
    for (CardResourceProfileConfigurator profile :
        configurator.getCardResourceProfileConfigurators()) {
      cardProfileNameToCardProfileManagerMap.put(
          profile.getProfileName(), new CardProfileManagerAdapter(profile, configurator));
    }
  }

  /**
   * Removes all reader managers whose reader is not accepted by any card profile manager and
   * unregisters their associated readers.
   */
  private void removeUnusedReaderManagers() {

    List<ReaderManagerAdapter> readerManagers = new ArrayList<>(readerToReaderManagerMap.values());

    for (ReaderManagerAdapter readerManager : readerManagers) {
      if (!readerManager.isActive()) {
        unregisterReader(readerManager.getReader(), readerManager.getPlugin());
      }
    }
  }

  /**
   * Removes the registered reader manager associated to the provided reader and stops the
   * observation of the reader if the reader is observable and the observation started.
   *
   * @param reader The reader to unregister.
   * @param plugin The associated plugin.
   */
  private void unregisterReader(CardReader reader, Plugin plugin) {

    readerToReaderManagerMap.remove(reader);
    Set<ObservableCardReader> usedObservableReaders = pluginToObservableReadersMap.get(plugin);

    if (usedObservableReaders != null && reader instanceof ObservableCardReader) {
      ObservableCardReader observableCardReader = (ObservableCardReader) reader;
      observableCardReader.removeObserver(this);
      usedObservableReaders.remove(observableCardReader);
    }
  }

  /**
   * Starts the observation of observable plugins and/or observable readers if requested.<br>
   * The observation of the readers is performed only for those accepted by at least one card
   * profile manager.
   */
  private void startMonitoring() {
    for (ConfiguredPlugin configuredPlugin : configurator.getConfiguredPlugins()) {

      if (configuredPlugin.isWithPluginMonitoring()
          && configuredPlugin.getPlugin() instanceof ObservablePlugin) {

        logger.info(
            "Plugin monitoring start requested [plugin={}]",
            configuredPlugin.getPlugin().getName());
        startPluginObservation(configuredPlugin);
      }

      if (configuredPlugin.isWithReaderMonitoring()
          && pluginToObservableReadersMap.containsKey(configuredPlugin.getPlugin())) {

        for (ObservableCardReader reader :
            pluginToObservableReadersMap.get(configuredPlugin.getPlugin())) {

          logger.info("Reader monitoring start requested [reader={}]", reader.getName());
          startReaderObservation(reader, configuredPlugin);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void stop() {
    isStarted = false;
    stopMonitoring();
    readerToReaderManagerMap.clear();
    cardProfileNameToCardProfileManagerMap.clear();
    cardResourceToPoolPluginMap.clear();
    pluginToObservableReadersMap.clear();
    logger.info("Card resource service stopped");
  }

  /** Stops the observation of all observable plugins and observable readers configured. */
  private void stopMonitoring() {
    for (ConfiguredPlugin configuredPlugin : configurator.getConfiguredPlugins()) {

      if (configuredPlugin.isWithPluginMonitoring()
          && configuredPlugin.getPlugin() instanceof ObservablePlugin) {

        ((ObservablePlugin) configuredPlugin.getPlugin()).removeObserver(this);
        logger.info(
            "Plugin monitoring stopped [plugin={}]", configuredPlugin.getPlugin().getName());
      }

      if (configuredPlugin.isWithReaderMonitoring()
          && pluginToObservableReadersMap.containsKey(configuredPlugin.getPlugin())) {

        for (ObservableCardReader reader :
            pluginToObservableReadersMap.get(configuredPlugin.getPlugin())) {

          reader.removeObserver(this);
          logger.info("Reader monitoring stopped [reader={}]", reader.getName());
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public CardResource getCardResource(String cardResourceProfileName) {

    if (logger.isDebugEnabled()) {
      logger.debug("Searching available card resource [profile={}]", cardResourceProfileName);
    }
    if (!isStarted) {
      throw new IllegalStateException("Card resource service not started");
    }
    Assert.getInstance().notEmpty(cardResourceProfileName, "cardResourceProfileName");

    CardProfileManagerAdapter cardProfileManager =
        cardProfileNameToCardProfileManagerMap.get(cardResourceProfileName);

    Assert.getInstance().notNull(cardProfileManager, "cardResourceProfileName");

    CardResource cardResource = cardProfileManager.getCardResource();

    if (logger.isDebugEnabled()) {
      logger.debug("Card resource found [cardResource={}]", getCardResourceInfo(cardResource));
    }

    return cardResource;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void releaseCardResource(CardResource cardResource) {

    if (logger.isDebugEnabled()) {
      logger.debug("Releasing card resource [cardResource={}]", getCardResourceInfo(cardResource));
    }
    if (!isStarted) {
      throw new IllegalStateException("Card resource service not started");
    }
    Assert.getInstance().notNull(cardResource, "cardResource");

    // For regular or pool plugin ?
    ReaderManagerAdapter readerManager =
        readerToReaderManagerMap.get(
            cardResource.getReader()); // NOSONAR card resource cannot be null here

    if (readerManager != null) {
      readerManager.unlock();

    } else {
      PoolPlugin poolPlugin = cardResourceToPoolPluginMap.get(cardResource);
      if (poolPlugin != null) {
        cardResourceToPoolPluginMap.remove(cardResource);
        poolPlugin.releaseReader(cardResource.getReader());
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Card resource released");
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void removeCardResource(CardResource cardResource) {
    releaseCardResource(cardResource);
    // For regular plugin ?
    ReaderManagerAdapter readerManager = readerToReaderManagerMap.get(cardResource.getReader());
    if (readerManager != null) {
      readerManager.removeCardResource(cardResource);
      for (CardProfileManagerAdapter cardProfileManager :
          cardProfileNameToCardProfileManagerMap.values()) {
        cardProfileManager.removeCardResource(cardResource);
      }
    }
    logger.info("Card resource removed [cardResource={}]", getCardResourceInfo(cardResource));
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void onPluginEvent(PluginEvent pluginEvent) {
    if (!isStarted) {
      return;
    }
    Plugin plugin = SmartCardServiceProvider.getService().getPlugin(pluginEvent.getPluginName());
    if (pluginEvent.getType() == PluginEvent.Type.READER_CONNECTED) {
      for (String readerName : pluginEvent.getReaderNames()) {
        // Get the new reader from the plugin because it is not yet registered in the service.
        CardReader reader = plugin.getReader(readerName);
        if (reader != null) {
          synchronized (reader) {
            onReaderConnected(reader, plugin);
          }
        }
      }
    } else {
      for (String readerName : pluginEvent.getReaderNames()) {
        // Get the reader back from the service because it is no longer registered in the plugin.
        CardReader reader = getReader(readerName);
        if (reader != null) {
          // The reader is registered in the service.
          synchronized (reader) {
            onReaderDisconnected(reader, plugin);
          }
        }
      }
    }
  }

  /**
   * Gets the reader having the provided name if it is registered.
   *
   * @param readerName The name of the reader.
   * @return Null if the reader is not or no longer registered.
   */
  private CardReader getReader(String readerName) {
    for (CardReader reader : readerToReaderManagerMap.keySet()) {
      if (reader.getName().equals(readerName)) {
        return reader;
      }
    }
    return null;
  }

  /**
   * Invoked when a new reader is connected.<br>
   * Notifies all card profile managers about the new available reader.<br>
   * If the new reader is accepted by at least one card profile manager, then a new reader manager
   * is registered to the service.
   *
   * @param reader The new reader.
   * @param plugin The associated plugin.
   */
  private void onReaderConnected(CardReader reader, Plugin plugin) {
    ReaderManagerAdapter readerManager = registerReader(reader, plugin);
    for (CardProfileManagerAdapter cardProfileManager :
        cardProfileNameToCardProfileManagerMap.values()) {
      cardProfileManager.onReaderConnected(readerManager);
    }
    if (readerManager.isActive()) {
      startMonitoring(reader, plugin);
    } else {
      unregisterReader(reader, plugin);
    }
  }

  /**
   * Starts the observation of the provided reader only if it is observable, if the monitoring is
   * requested for the provided plugin and if the reader is accepted by at least one card profile
   * manager.
   *
   * @param reader The reader to observe.
   * @param plugin The associated plugin.
   */
  private void startMonitoring(CardReader reader, Plugin plugin) {

    if (reader instanceof ObservableCardReader) {

      for (ConfiguredPlugin configuredPlugin : configurator.getConfiguredPlugins()) {

        if (configuredPlugin.getPlugin() == plugin && configuredPlugin.isWithReaderMonitoring()) {

          logger.info("Reader monitoring start requested [reader={}]", reader.getName());
          startReaderObservation((ObservableCardReader) reader, configuredPlugin);
        }
      }
    }
  }

  /**
   * Starts the observation of the "regular" plugin.
   *
   * @param configuredPlugin The associated configuration.
   */
  private void startPluginObservation(ConfiguredPlugin configuredPlugin) {
    ObservablePlugin observablePlugin = (ObservablePlugin) configuredPlugin.getPlugin();
    observablePlugin.setPluginObservationExceptionHandler(
        configuredPlugin.getPluginObservationExceptionHandlerSpi());
    observablePlugin.addObserver(this);
  }

  /**
   * Starts the observation of the reader associated to a "regular" plugin.
   *
   * @param observableReader The observable reader to observe.
   * @param configuredPlugin The associated configuration.
   */
  private void startReaderObservation(
      ObservableCardReader observableReader, ConfiguredPlugin configuredPlugin) {
    observableReader.setReaderObservationExceptionHandler(
        configuredPlugin.getReaderObservationExceptionHandlerSpi());
    observableReader.addObserver(this);
    observableReader.startCardDetection(ObservableCardReader.DetectionMode.REPEATING);
  }

  /**
   * Invoked when an accepted reader is no more available because it was disconnected or
   * unregistered.<br>
   * Removes its reader manager and all associated created card resources from all card profile
   * managers.
   *
   * @param reader The disconnected reader.
   * @param plugin The associated plugin.
   */
  private void onReaderDisconnected(CardReader reader, Plugin plugin) {
    ReaderManagerAdapter readerManager = readerToReaderManagerMap.get(reader);
    if (readerManager != null) {
      logger.info(
          "Removing disconnected reader and all associated card resources [reader={}]",
          reader.getName());
      onCardRemoved(readerManager);
      unregisterReader(reader, plugin);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.0.0
   */
  @Override
  public void onReaderEvent(CardReaderEvent readerEvent) {
    if (!isStarted) {
      return;
    }
    CardReader reader = getReader(readerEvent.getReaderName());
    if (reader != null) {
      // The reader is registered in the service.
      synchronized (reader) {
        ReaderManagerAdapter readerManager = readerToReaderManagerMap.get(reader);
        if (readerManager != null) {
          onReaderEvent(readerEvent, readerManager);
        }
      }
    }
  }

  /**
   * Invoked when a card is inserted, removed or the associated reader unregistered.<br>
   *
   * @param readerEvent The reader event.
   * @param readerManager The reader manager associated to the reader.
   */
  private void onReaderEvent(CardReaderEvent readerEvent, ReaderManagerAdapter readerManager) {
    if (readerEvent.getType() == CardReaderEvent.Type.CARD_INSERTED
        || readerEvent.getType() == CardReaderEvent.Type.CARD_MATCHED) {
      logger.info(
          "Creating new card resources matching the new card inserted [reader={}]",
          readerManager.getReader().getName());
      onCardInserted(readerManager);
    } else {
      logger.info(
          "Removing all card resources caused by a card removal or reader unregistration [reader={}]",
          readerManager.getReader().getName());
      onCardRemoved(readerManager);
    }
  }

  /**
   * Invoked when a card is inserted on a reader.<br>
   * Notifies all card profile managers about the insertion of the card.<br>
   * Each card profile manager interested in the card reader will try to create a card resource.
   *
   * @param readerManager The associated reader manager.
   */
  private void onCardInserted(ReaderManagerAdapter readerManager) {
    for (CardProfileManagerAdapter cardProfileManager :
        cardProfileNameToCardProfileManagerMap.values()) {
      cardProfileManager.onCardInserted(readerManager);
    }
  }

  /**
   * Invoked when a card is removed or the associated reader unregistered.<br>
   * Removes all created card resources associated to the reader.
   *
   * @param readerManager The associated reader manager.
   */
  private void onCardRemoved(ReaderManagerAdapter readerManager) {

    Set<CardResource> cardResourcesToRemove = new HashSet<>(readerManager.getCardResources());

    for (CardResource cardResource : cardResourcesToRemove) {
      removeCardResource(cardResource);
    }
  }
}
