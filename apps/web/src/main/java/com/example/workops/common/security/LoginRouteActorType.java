package com.example.workops.common.security;

/** OAuth2 Login registrationとWorkOps利用者区分の対応を表す。 */
public enum LoginRouteActorType {
  PLATFORM("platform", "PLATFORM"),
  TENANT("tenant", "TENANT");

  private final String registrationId;
  private final String actorType;

  LoginRouteActorType(String registrationId, String actorType) {
    this.registrationId = registrationId;
    this.actorType = actorType;
  }

  public String registrationId() {
    return registrationId;
  }

  public String actorType() {
    return actorType;
  }

  public boolean matches(String actualActorType) {
    return actorType.equals(actualActorType);
  }

  public static LoginRouteActorType fromRegistrationId(String registrationId) {
    for (LoginRouteActorType routeActorType : values()) {
      if (routeActorType.registrationId.equals(registrationId)) {
        return routeActorType;
      }
    }

    throw new IllegalStateException(
        "OAuth2 Login registrationId is not supported: " + registrationId);
  }
}
