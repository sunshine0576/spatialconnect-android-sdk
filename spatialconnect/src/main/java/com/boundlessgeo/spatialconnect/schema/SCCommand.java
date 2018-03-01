package com.boundlessgeo.spatialconnect.schema;

public enum SCCommand {

	NO_ACTION(0),
	START_ALL_SERVICES(1),
	DATASERVICE_ACTIVESTORESLIST(100),
	DATASERVICE_ACTIVESTOREBYID(101),
	DATASERVICE_STORELIST(102),
	DATASERVICE_QUERY(110),
	DATASERVICE_QUERYALL(111),
	DATASERVICE_SPATIALQUERY(112),
	DATASERVICE_SPATIALQUERYALL(113),
	DATASERVICE_CREATEFEATURE(114),
	DATASERVICE_UPDATEFEATURE(115),
	DATASERVICE_DELETEFEATURE(116),
	DATASERVICE_FORMLIST(117),
	SENSORSERVICE_GPS(200),
	AUTHSERVICE_AUTHENTICATE(300),
	AUTHSERVICE_LOGOUT(301),
	AUTHSERVICE_ACCESS_TOKEN(302),
	AUTHSERVICE_LOGIN_STATUS(303),
	NETWORKSERVICE_GET_REQUEST(400),
	NETWORKSERVICE_POST_REQUEST(401),
	CONFIG_FULL(500),
	CONFIG_STORE_LIST(501),
	CONFIG_ADD_STORE(502),
	CONFIG_REMOVE_STORE(503),
	CONFIG_UPDATE_STORE(504),
	CONFIG_FORM_LIST(505),
	CONFIG_ADD_FORM(506),
	CONFIG_REMOVE_FORM(507),
	CONFIG_UPDATE_FORM(508),
	CONFIG_REGISTER_DEVICE(509),
	NOTIFICATIONS(600),
	NOTIFICATION_ALERT(601),
	NOTIFICATION_INFO(602),
	NOTIFICATION_CONTENT_AVAILABLE(602),
	BACKENDSERVICE_HTTP_URI(701),
	BACKENDSERVICE_MQTT_CONNECTED(702);

    private final int actionNumber;

    SCCommand(int actionNumber) {
        this.actionNumber = actionNumber;
    }

    public int value() {
        return actionNumber;
    }

    public static SCCommand fromActionNumber(int actionNumber) {
        for (SCCommand v : values()) {
            if (v.actionNumber == actionNumber) {
                return v;
            }
        }
        throw new IllegalArgumentException(
            String.valueOf(actionNumber) + " is not an action number associated with a SCCommand."
        );
    }

};