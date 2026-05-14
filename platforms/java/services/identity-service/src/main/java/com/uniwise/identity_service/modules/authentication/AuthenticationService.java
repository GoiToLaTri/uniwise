package com.uniwise.identity_service.modules.authentication;

import java.util.List;

import com.uniwise.common.dto.request.GetTokenRequest;
import com.uniwise.common.dto.request.RefreshTokenRequest;
import com.uniwise.common.dto.response.SessionResponse;
import com.uniwise.common.dto.response.TokenResponse;

public interface AuthenticationService {
    // Đăng nhập
    TokenResponse getToken(GetTokenRequest request);

    // Làm mới token
    TokenResponse refresh(RefreshTokenRequest request);

    // Đăng xuất (Vô hiệu hóa session hiện tại)
    void logout(RefreshTokenRequest request);

    // Xem danh sách các phiên đang hoạt động của user
    List<SessionResponse> getActiveSessions(String accountId, String currentSessionId);

    // Thu hồi một phiên cụ thể (Ví dụ: Đăng xuất từ xa một thiết bị khác)
    void revokeSession(String sessionId);
}
