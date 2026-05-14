package com.uniwise.identity_service.modules.refreshtoken;

import com.uniwise.identity_service.modules.refreshtoken.entity.RefreshToken;
import com.uniwise.identity_service.modules.session.entity.Session;

public interface RefreshTokenService {
    // Tạo mới một Refresh Token cho một phiên đăng nhập
    RefreshToken create(Session session, String rawToken);

    // Lấy thông tin token dựa trên chuỗi băm (hash)
    RefreshToken getByHash(String hash);

    // Đánh dấu token đã được sử dụng (khi thực hiện refresh thành công)
    void markUsed(RefreshToken token);

    // Đánh dấu token/session bị xâm nhập (khi phát hiện reuse)
    void markCompromised(RefreshToken token);
}
