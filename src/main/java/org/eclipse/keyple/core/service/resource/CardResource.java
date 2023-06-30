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

import org.eclipse.keyple.core.common.KeypleReaderExtension;
import org.eclipse.keypop.reader.CardReader;
import org.eclipse.keypop.reader.selection.spi.SmartCard;

/**
 * This POJO contains a smart card and its associated card reader.
 *
 * @since 2.0.0
 */
public interface CardResource {

  /**
   * Returns the reader.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  CardReader getReader();

  /**
   * Returns the Keyple reader's extension.
   *
   * @return A not null reference.
   * @since 2.1.0
   */
  KeypleReaderExtension getReaderExtension();

  /**
   * Returns the smart card image.
   *
   * @return A not null reference.
   * @since 2.0.0
   */
  SmartCard getSmartCard();
}
