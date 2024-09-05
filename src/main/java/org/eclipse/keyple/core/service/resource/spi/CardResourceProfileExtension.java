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
package org.eclipse.keyple.core.service.resource.spi;

import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.ReaderApiFactory;
import org.eclipse.keypop.reader.selection.spi.SmartCard;

/**
 * Provides means to check if a reader contains a card that matches a given profile.
 *
 * @since 2.0.0
 */
public interface CardResourceProfileExtension {

  /**
   * Checks if a card is inserted in the provided reader, selects it, evaluates its profile and
   * potentially executes any necessary commands.
   *
   * @param reader The reader in which the card is supposed to be inserted.
   * @param readerApiFactory A instance of {@link ReaderApiFactory}.
   * @return A {@link SmartCard} or null if no card is inserted or if its profile does not match the
   *     associated data.
   * @since 2.0.0
   */
  SmartCard matches(CardReader reader, ReaderApiFactory readerApiFactory);

  /**
   * Checks if the provided selected smart card is as expected by the profile.
   *
   * @param smartCard The selected smart card.
   * @return The provided {@link SmartCard} or null if its profile does not match the associated
   *     data.
   * @since 3.1.0
   */
  SmartCard matches(SmartCard smartCard);
}
