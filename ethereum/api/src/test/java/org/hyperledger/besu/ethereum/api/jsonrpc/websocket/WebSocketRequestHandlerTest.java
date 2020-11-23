/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.api.jsonrpc.websocket;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.hyperledger.besu.ethereum.api.handlers.TimeoutOptions;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.JsonRpcRequest;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcError;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcErrorResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.hyperledger.besu.ethereum.api.jsonrpc.websocket.methods.WebSocketRpcRequest;
import org.hyperledger.besu.ethereum.eth.manager.EthScheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(VertxUnitRunner.class)
public class WebSocketRequestHandlerTest {

  private static final int VERTX_AWAIT_TIMEOUT_MILLIS = 10000;

  private Vertx vertx;
  private WebSocketRequestHandler handler;
  private JsonRpcMethod jsonRpcMethodMock;
  private final Map<String, JsonRpcMethod> methods = new HashMap<>();

  @Before
  public void before(final TestContext context) {
    vertx = Vertx.vertx();

    jsonRpcMethodMock = mock(JsonRpcMethod.class);

    methods.put("eth_x", jsonRpcMethodMock);
    handler =
        new WebSocketRequestHandler(
            vertx,
            methods,
            mock(EthScheduler.class),
            TimeoutOptions.defaultOptions().getTimeoutSeconds());
  }

  @After
  public void after(final TestContext context) {
    Mockito.reset(jsonRpcMethodMock);
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void handlerDeliversResponseSuccessfully(final TestContext context) {
    final Async async = context.async();

    final JsonObject requestJson = new JsonObject().put("id", 1).put("method", "eth_x");
    final JsonRpcRequest requestBody = requestJson.mapTo(WebSocketRpcRequest.class);
    final JsonRpcRequestContext expectedRequest = new JsonRpcRequestContext(requestBody);

    final JsonRpcSuccessResponse expectedResponse =
        new JsonRpcSuccessResponse(requestBody.getId(), null);

    when(jsonRpcMethodMock.response(eq(expectedRequest))).thenReturn(expectedResponse);

    final String websocketId = UUID.randomUUID().toString();

    vertx
        .eventBus()
        .consumer(websocketId)
        .handler(
            msg -> {
              context.assertEquals(Json.encode(expectedResponse), msg.body());
              async.complete();
            })
        .completionHandler(v -> handler.handle(websocketId, requestJson.toString()));

    async.awaitSuccess(WebSocketRequestHandlerTest.VERTX_AWAIT_TIMEOUT_MILLIS);
    // can verify only after async not before
    verify(jsonRpcMethodMock).response(eq(expectedRequest));
  }

  @Test
  public void handlerBatchRequestDeliversResponseSuccessfully(final TestContext context) {
    final Async async = context.async();

    final JsonObject requestJson = new JsonObject().put("id", 1).put("method", "eth_x");
    final JsonArray arrayJson = new JsonArray(List.of(requestJson, requestJson));
    final JsonRpcRequest requestBody = requestJson.mapTo(WebSocketRpcRequest.class);
    final JsonRpcRequestContext expectedRequest = new JsonRpcRequestContext(requestBody);
    final JsonRpcSuccessResponse expectedSingleResponse =
        new JsonRpcSuccessResponse(requestBody.getId(), null);

    final JsonArray expectedBatchResponse =
        new JsonArray(List.of(expectedSingleResponse, expectedSingleResponse));

    when(jsonRpcMethodMock.response(eq(expectedRequest))).thenReturn(expectedSingleResponse);

    final String websocketId = UUID.randomUUID().toString();

    vertx
        .eventBus()
        .consumer(websocketId)
        .handler(
            msg -> {
              context.assertEquals(Json.encode(expectedBatchResponse), msg.body());
              async.complete();
            })
        .completionHandler(v -> handler.handle(websocketId, arrayJson.toString()));

    async.awaitSuccess(WebSocketRequestHandlerTest.VERTX_AWAIT_TIMEOUT_MILLIS);
    // can verify only after async not before
    verify(jsonRpcMethodMock, Mockito.times(2)).response(eq(expectedRequest));
  }

  @Test
  public void handlerBatchRequestContainingErrorsShouldRespondWithBatchErrors(
      final TestContext context) {
    final Async async = context.async();

    final JsonObject requestJson =
        new JsonObject().put("id", 1).put("method", "eth_nonexistentMethod");
    final JsonRpcErrorResponse expectedErrorResponse1 =
        new JsonRpcErrorResponse(1, JsonRpcError.METHOD_NOT_FOUND);

    final JsonArray arrayJson = new JsonArray(List.of(requestJson, ""));
    final JsonRpcErrorResponse expectedErrorResponse2 =
        new JsonRpcErrorResponse(null, JsonRpcError.INVALID_REQUEST);

    final JsonArray expectedBatchResponse =
        new JsonArray(List.of(expectedErrorResponse1, expectedErrorResponse2));

    final String websocketId = UUID.randomUUID().toString();

    vertx
        .eventBus()
        .consumer(websocketId)
        .handler(
            msg -> {
              context.assertEquals(Json.encode(expectedBatchResponse), msg.body());
              async.complete();
            })
        .completionHandler(v -> handler.handle(websocketId, arrayJson.toString()));

    async.awaitSuccess(WebSocketRequestHandlerTest.VERTX_AWAIT_TIMEOUT_MILLIS);
  }

  @Test
  public void jsonDecodeFailureShouldRespondInvalidRequest(final TestContext context) {
    final Async async = context.async();

    final JsonRpcErrorResponse expectedResponse =
        new JsonRpcErrorResponse(null, JsonRpcError.INVALID_REQUEST);

    final String websocketId = UUID.randomUUID().toString();

    vertx
        .eventBus()
        .consumer(websocketId)
        .handler(
            msg -> {
              context.assertEquals(Json.encode(expectedResponse), msg.body());
              verifyZeroInteractions(jsonRpcMethodMock);
              async.complete();
            })
        .completionHandler(v -> handler.handle(websocketId, ""));

    async.awaitSuccess(VERTX_AWAIT_TIMEOUT_MILLIS);
  }

  @Test
  public void objectMapperFailureShouldRespondInvalidRequest(final TestContext context) {
    final Async async = context.async();

    final JsonRpcErrorResponse expectedResponse =
        new JsonRpcErrorResponse(null, JsonRpcError.INVALID_REQUEST);

    final String websocketId = UUID.randomUUID().toString();

    vertx
        .eventBus()
        .consumer(websocketId)
        .handler(
            msg -> {
              context.assertEquals(Json.encode(expectedResponse), msg.body());
              verifyZeroInteractions(jsonRpcMethodMock);
              async.complete();
            })
        .completionHandler(v -> handler.handle(websocketId, "{}"));

    async.awaitSuccess(VERTX_AWAIT_TIMEOUT_MILLIS);
  }

  @Test
  public void absentMethodShouldRespondMethodNotFound(final TestContext context) {
    final Async async = context.async();

    final JsonObject requestJson =
        new JsonObject().put("id", 1).put("method", "eth_nonexistentMethod");
    final JsonRpcErrorResponse expectedResponse =
        new JsonRpcErrorResponse(1, JsonRpcError.METHOD_NOT_FOUND);

    final String websocketId = UUID.randomUUID().toString();

    vertx
        .eventBus()
        .consumer(websocketId)
        .handler(
            msg -> {
              context.assertEquals(Json.encode(expectedResponse), msg.body());
              async.complete();
            })
        .completionHandler(v -> handler.handle(websocketId, requestJson.toString()));

    async.awaitSuccess(WebSocketRequestHandlerTest.VERTX_AWAIT_TIMEOUT_MILLIS);
  }

  @Test
  public void onInvalidJsonRpcParametersExceptionProcessingRequestShouldRespondInvalidParams(
      final TestContext context) {
    final Async async = context.async();

    final JsonObject requestJson = new JsonObject().put("id", 1).put("method", "eth_x");
    final JsonRpcRequestContext expectedRequest =
        new JsonRpcRequestContext(requestJson.mapTo(WebSocketRpcRequest.class));
    when(jsonRpcMethodMock.response(eq(expectedRequest)))
        .thenThrow(new InvalidJsonRpcParameters(""));
    final JsonRpcErrorResponse expectedResponse =
        new JsonRpcErrorResponse(1, JsonRpcError.INVALID_PARAMS);

    final String websocketId = UUID.randomUUID().toString();

    vertx
        .eventBus()
        .consumer(websocketId)
        .handler(
            msg -> {
              context.assertEquals(Json.encode(expectedResponse), msg.body());
              async.complete();
            })
        .completionHandler(v -> handler.handle(websocketId, requestJson.toString()));

    async.awaitSuccess(WebSocketRequestHandlerTest.VERTX_AWAIT_TIMEOUT_MILLIS);
  }

  @Test
  public void onExceptionProcessingRequestShouldRespondInternalError(final TestContext context) {
    final Async async = context.async();

    final JsonObject requestJson = new JsonObject().put("id", 1).put("method", "eth_x");
    final JsonRpcRequestContext expectedRequest =
        new JsonRpcRequestContext(requestJson.mapTo(WebSocketRpcRequest.class));
    when(jsonRpcMethodMock.response(eq(expectedRequest))).thenThrow(new RuntimeException());
    final JsonRpcErrorResponse expectedResponse =
        new JsonRpcErrorResponse(1, JsonRpcError.INTERNAL_ERROR);

    final String websocketId = UUID.randomUUID().toString();

    vertx
        .eventBus()
        .consumer(websocketId)
        .handler(
            msg -> {
              context.assertEquals(Json.encode(expectedResponse), msg.body());
              async.complete();
            })
        .completionHandler(v -> handler.handle(websocketId, requestJson.toString()));

    async.awaitSuccess(WebSocketRequestHandlerTest.VERTX_AWAIT_TIMEOUT_MILLIS);
  }
}
