/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket.api.provider.tcp;

import static org.mule.extension.socket.api.SocketsExtension.TLS;
import static org.mule.extension.socket.api.SocketsExtension.TLS_CONFIGURATION;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.ADVANCED;
import static org.mule.runtime.extension.api.annotation.param.ParameterGroup.CONNECTION;
import org.mule.extension.socket.api.SocketConnectionSettings;
import org.mule.extension.socket.api.connection.tcp.TcpListenerConnection;
import org.mule.extension.socket.api.connection.tcp.protocol.SafeProtocol;
import org.mule.extension.socket.api.socket.factory.SimpleServerSocketFactory;
import org.mule.extension.socket.api.socket.factory.SslServerSocketFactory;
import org.mule.extension.socket.api.socket.factory.TcpServerSocketFactory;
import org.mule.extension.socket.api.socket.tcp.TcpProtocol;
import org.mule.extension.socket.api.socket.tcp.TcpServerSocketProperties;
import org.mule.extension.socket.api.source.SocketListener;
import org.mule.extension.socket.internal.SocketUtils;
import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.net.ServerSocket;

import javax.net.ssl.SSLServerSocket;


/**
 * A {@link ConnectionProvider} which provides instances of {@link TcpListenerConnection} to be used by {@link SocketListener}
 *
 * @since 1.0
 */
@Alias("tcp-listener")
public final class TcpListenerProvider implements CachedConnectionProvider<TcpListenerConnection>, Initialisable {

  /**
   * Its presence will imply the use of {@link SSLServerSocket} instead of plain TCP {@link ServerSocket} for accepting new SSL
   * connections.
   */
  @Parameter
  @Optional
  @DisplayName(TLS_CONFIGURATION)
  @Placement(tab = TLS)
  @Summary("Its presence will imply the use of SSLServerSocket instead of plain TCP")
  private TlsContextFactory tlsContext;

  /**
   * This configuration parameter refers to the address where the TCP socket should listen for incoming connections.
   */
  @ParameterGroup(name = CONNECTION)
  private SocketConnectionSettings connectionSettings;

  /**
   * {@link ServerSocket} configuration properties
   */
  @ParameterGroup(name = ADVANCED)
  private TcpServerSocketProperties tcpServerSocketProperties;

  /**
   * {@link TcpProtocol} that knows how the data is going to be read and written. If not specified, the {@link SafeProtocol} will
   * be used.
   */
  @Parameter
  @Optional
  @Summary("TCP Protocol to use to receive external request")
  @NullSafe(defaultImplementingType = SafeProtocol.class)
  private TcpProtocol protocol;

  @Override
  public TcpListenerConnection connect() throws ConnectionException {
    SimpleServerSocketFactory serverSocketFactory = null;

    try {
      serverSocketFactory = tlsContext != null ? new SslServerSocketFactory(tlsContext) : new TcpServerSocketFactory();
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }

    TcpListenerConnection connection =
        new TcpListenerConnection(connectionSettings, protocol, tcpServerSocketProperties, serverSocketFactory);
    connection.connect();

    return connection;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void disconnect(TcpListenerConnection connection) {
    connection.disconnect();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectionValidationResult validate(TcpListenerConnection connection) {
    return SocketUtils.validate(connection);
  }

  @Override
  public void initialise() throws InitialisationException {
    if (tlsContext != null && !tlsContext.isKeyStoreConfigured()) {
      throw new InitialisationException(CoreMessages.createStaticMessage("KeyStore must be configured for server side SSL"),
                                        this);
    }

    initialiseIfNeeded(tlsContext);
  }
}
