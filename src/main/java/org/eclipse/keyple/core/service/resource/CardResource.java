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

import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.selection.spi.SmartCard;
import org.eclipse.keyple.core.util.Assert;

/**
 * This POJO contains a {@link SmartCard} and its associated {@link CardReader}.
 *
 * @since 2.0.0
 */
public final class CardResource {

  private final CardReader reader;
  private final SmartCard smartCard;

  /**
   * Creates an instance of {@link CardResource}.
   *
   * @param reader The {@link CardReader}.
   * @param smartCard The {@link SmartCard}.
   * @since 2.0.0
   */
  public CardResource(CardReader reader, SmartCard smartCard) {

    Assert.getInstance().notNull(reader, "reader").notNull(smartCard, "smartCard");

    this.reader = reader;
    this.smartCard = smartCard;
  }

  /**
   * Gets the reader
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  public CardReader getReader() {
    return reader;
  }

  /**
   * Gets the {@link SmartCard}.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  public SmartCard getSmartCard() {
    return smartCard;
  }
}
