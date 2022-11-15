package org.sdase.commons.client.jersey.proxy.annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sdase.commons.client.jersey.proxy.ApiClientInvocationHandler.createProxy;

import java.util.stream.Stream;
import javax.ws.rs.WebApplicationException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sdase.commons.client.jersey.error.ClientRequestException;

@ExtendWith(MockitoExtension.class)
class SuppressHttpErrorsToNullTest<T extends SuppressHttpErrorsToNullTest.TestApi> {

  private static Stream<Arguments> data() {
    return Stream.of(
        arguments(ClientErrorsSuppressed.class, 404, false),
        arguments(ClientErrorsSuppressed.class, 500, true),
        arguments(ServerErrorsSuppressed.class, 500, false),
        arguments(ServerErrorsSuppressed.class, 400, true),
        arguments(ServerErrorsAndNotFoundSuppressed.class, 500, false),
        arguments(ServerErrorsAndNotFoundSuppressed.class, 404, false),
        arguments(ServerErrorsAndNotFoundSuppressed.class, 400, true),
        arguments(NotFoundAndForbiddenSuppressed.class, 403, false),
        arguments(NotFoundAndForbiddenSuppressed.class, 404, false),
        arguments(NotFoundAndForbiddenSuppressed.class, 400, true),
        arguments(RedirectErrorsSuppressed.class, 303, false),
        arguments(RedirectErrorsSuppressed.class, 400, true));
  }

  @ParameterizedTest
  @MethodSource("data")
  void exceptionForObject(Class<T> testApi, int givenHttpError, boolean expectException) {
    WebApplicationException error = new WebApplicationException(givenHttpError);
    T mockApi = mock(testApi);
    when(mockApi.suppressed()).thenThrow(error);
    var testApiImpl = createProxy(testApi, mockApi);

    if (expectException) {
      assertThatExceptionOfType(ClientRequestException.class).isThrownBy(testApiImpl::suppressed);
    } else {
      assertThat(testApiImpl.suppressed()).isNull();
    }
  }

  @ParameterizedTest
  @MethodSource("data")
  void exceptionForVoid(Class<T> testApi, int givenHttpError, boolean expectException) {
    WebApplicationException error = new WebApplicationException(givenHttpError);
    T mockApi = mock(testApi);
    doThrow(error).when(mockApi).suppressedForVoid();
    var testApiImpl = createProxy(testApi, mockApi);

    if (expectException) {
      assertThatExceptionOfType(ClientRequestException.class)
          .isThrownBy(testApiImpl::suppressedForVoid);
    } else {
      assertThatCode(testApiImpl::suppressedForVoid).doesNotThrowAnyException();
    }
  }

  interface TestApi {
    Object suppressed();

    void suppressedForVoid();
  }

  public interface ClientErrorsSuppressed extends TestApi {
    @SuppressHttpErrorsToNull(allClientErrors = true)
    Object suppressed();

    @SuppressHttpErrorsToNull(allClientErrors = true)
    void suppressedForVoid();
  }

  public interface ServerErrorsSuppressed extends TestApi {
    @SuppressHttpErrorsToNull(allServerErrors = true)
    Object suppressed();

    @SuppressHttpErrorsToNull(allServerErrors = true)
    void suppressedForVoid();
  }

  public interface ServerErrorsAndNotFoundSuppressed extends TestApi {

    @SuppressHttpErrorsToNull(value = 404, allServerErrors = true)
    Object suppressed();

    @SuppressHttpErrorsToNull(value = 404, allServerErrors = true)
    void suppressedForVoid();
  }

  public interface NotFoundAndForbiddenSuppressed extends TestApi {

    @SuppressHttpErrorsToNull(value = {403, 404})
    Object suppressed();

    @SuppressHttpErrorsToNull(value = {403, 404})
    void suppressedForVoid();
  }

  public interface RedirectErrorsSuppressed extends TestApi {

    @SuppressHttpErrorsToNull(allRedirectErrors = true)
    Object suppressed();

    @SuppressHttpErrorsToNull(allRedirectErrors = true)
    void suppressedForVoid();
  }
}
