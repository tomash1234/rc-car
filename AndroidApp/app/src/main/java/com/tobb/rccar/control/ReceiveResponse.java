package com.tobb.rccar.control;

public interface ReceiveResponse {

    void receive(int tag, String content, int responsCode, boolean error);
}
