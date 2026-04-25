package com.cloud.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloud.api.auth.AuthGovernanceDubboApi;
import com.cloud.api.order.OrderDubboApi;
import com.cloud.api.payment.PaymentDubboApi;
import com.cloud.api.product.ProductDubboApi;
import com.cloud.api.stock.StockDubboApi;
import com.cloud.api.user.AdminGovernanceDubboApi;
import com.cloud.api.user.UserAdminGovernanceDubboApi;
import com.cloud.api.user.UserGovernanceDubboApi;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RpcReturnSerializableContractTest {

  private static final List<Class<?>> DUBBO_APIS =
      List.of(
          AuthGovernanceDubboApi.class,
          OrderDubboApi.class,
          PaymentDubboApi.class,
          ProductDubboApi.class,
          StockDubboApi.class,
          AdminGovernanceDubboApi.class,
          UserAdminGovernanceDubboApi.class,
          UserGovernanceDubboApi.class);

  @Test
  void dubboReturnGraphShouldRemainSerializable() {
    List<String> violations = new ArrayList<>();
    for (Class<?> api : DUBBO_APIS) {
      for (Method method : api.getMethods()) {
        inspectType(
            method.getGenericReturnType(),
            api.getName() + "#" + method.getName() + "(return)",
            violations,
            new java.util.HashSet<>());
      }
    }

    assertTrue(violations.isEmpty(), () -> String.join(System.lineSeparator(), violations));
  }

  private static void inspectType(
      Type type, String path, List<String> violations, Set<String> visitedTypes) {
    if (type instanceof Class<?> clazz) {
      inspectClass(clazz, path, violations, visitedTypes);
      return;
    }
    if (type instanceof ParameterizedType parameterizedType) {
      inspectType(parameterizedType.getRawType(), path, violations, visitedTypes);
      for (Type actualTypeArgument : parameterizedType.getActualTypeArguments()) {
        inspectType(actualTypeArgument, path, violations, visitedTypes);
      }
      return;
    }
    if (type instanceof GenericArrayType genericArrayType) {
      inspectType(
          genericArrayType.getGenericComponentType(), path + "[]", violations, visitedTypes);
      return;
    }
    if (type instanceof WildcardType wildcardType) {
      for (Type upperBound : wildcardType.getUpperBounds()) {
        inspectType(upperBound, path, violations, visitedTypes);
      }
      for (Type lowerBound : wildcardType.getLowerBounds()) {
        inspectType(lowerBound, path, violations, visitedTypes);
      }
      return;
    }
    if (type instanceof TypeVariable<?>) {
      return;
    }
  }

  private static void inspectClass(
      Class<?> clazz, String path, List<String> violations, Set<String> visitedTypes) {
    if (clazz.isPrimitive() || clazz.isEnum()) {
      return;
    }
    if (clazz.isArray()) {
      inspectClass(clazz.getComponentType(), path + "[]", violations, visitedTypes);
      return;
    }
    if (Collection.class.isAssignableFrom(clazz)
        || Map.class.isAssignableFrom(clazz)
        || clazz.getName().startsWith("java.")
        || clazz.getName().startsWith("jakarta.")) {
      return;
    }

    if (!visitedTypes.add(clazz.getName())) {
      return;
    }

    if (clazz.getName().startsWith("com.cloud.common.domain")
        && !Serializable.class.isAssignableFrom(clazz)) {
      violations.add(path + " uses non-serializable type: " + clazz.getName());
    }

    if (!clazz.getName().startsWith("com.cloud.common.domain")) {
      return;
    }

    for (Field field : clazz.getDeclaredFields()) {
      if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
        continue;
      }
      inspectType(field.getGenericType(), path + "." + field.getName(), violations, visitedTypes);
    }
  }
}
