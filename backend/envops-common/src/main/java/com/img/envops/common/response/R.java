package com.img.envops.common.response;

public class R<T> {
  private String code;
  private String msg;
  private T data;

  public R() {
  }

  public R(String code, String msg, T data) {
    this.code = code;
    this.msg = msg;
    this.data = data;
  }

  public static <T> R<T> ok(T data) {
    return new R<>("0000", "success", data);
  }

  public static <T> R<T> fail(String code, String msg) {
    return new R<>(code, msg, null);
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }
}
