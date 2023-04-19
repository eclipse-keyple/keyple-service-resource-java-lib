/* **************************************************************************************
 * Copyright (c) 2023 Calypso Networks Association https://calypsonet.org/
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
import org.eclipse.keyple.core.common.KeypleReaderExtension;

/**
 * Adapter of {@link CardResource}.
 *
 * @since 2.1.0
 */
final class CardResourceAdapter implements CardResource {

  private final CardReader reader;
  private final KeypleReaderExtension readerExtension;
  private final SmartCard smartCard;

  /**
   * Creates new instance.
   *
   * @param reader The card reader.
   * @param readerExtension The Keyple reader's extension.
   * @param smartCard The smart card image.
   * @since 2.1.0
   */
  CardResourceAdapter(
      CardReader reader, KeypleReaderExtension readerExtension, SmartCard smartCard) {
    this.reader = reader;
    this.readerExtension = readerExtension;
    this.smartCard = smartCard;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.1.0
   */
  @Override
  public CardReader getReader() {
    return reader;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.1.0
   */
  @Override
  public KeypleReaderExtension getReaderExtension() {
    return readerExtension;
  }

  /**
   * {@inheritDoc}
   *
   * @since 2.1.0
   */
  @Override
  public SmartCard getSmartCard() {
    return smartCard;
  }
}
