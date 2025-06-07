package com.dungochung.shopdongho.dto;

public class ResponseDataDto {
	private int responseCode;
	private String responseMsg;
	private Object data;

	public ResponseDataDto() {
	}

	public ResponseDataDto(int responseCode, String responseMsg) {
		this.responseCode = responseCode;
		this.responseMsg = responseMsg;
	}

	public ResponseDataDto(int responseCode, String responseMsg, Object data) {
		this.responseCode = responseCode;
		this.responseMsg = responseMsg;
		this.data = data;
	}

	/**
	 * @return the responseCode
	 */
	public int getResponseCode() {
		return responseCode;
	}

	/**
	 * @param responseCode the responseCode to set
	 */
	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}

	/**
	 * @return the responseMsg
	 */
	public String getResponseMsg() {
		return responseMsg;
	}

	/**
	 * @param responseMsg the responseMsg to set
	 */
	public void setResponseMsg(String responseMsg) {
		this.responseMsg = responseMsg;
	}

	/**
	 * @return the data
	 */
	public Object getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(Object data) {
		this.data = data;
	}
}
