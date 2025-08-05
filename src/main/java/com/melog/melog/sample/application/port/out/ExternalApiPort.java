package com.melog.melog.sample.application.port.out;

import com.melog.melog.sample.domain.model.request.SampleRestRequest;
import com.melog.melog.sample.domain.model.response.SampleRestResponse;

public interface ExternalApiPort {
    SampleRestResponse callExternalApi(SampleRestRequest request);
}
