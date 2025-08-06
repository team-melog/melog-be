package com.melog.melog.clova.application.port.out;

import com.melog.melog.clova.domain.model.request.ClovaStudioRequest;
import com.melog.melog.clova.domain.model.response.ClovaStudioResponse;

public interface ClovaStudioPort {
    ClovaStudioResponse sendRequest(ClovaStudioRequest request);
}
