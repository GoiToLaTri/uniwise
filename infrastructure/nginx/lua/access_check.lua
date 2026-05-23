local sha256 = require "resty.sha256"
local str = require "resty.string"
local redis = require "resty.redis"
local cjson = require "cjson"
local jwt = require "resty.jwt"

local function register_public_endpoint()
    local public_endpoints = {
        "/identity-service/api/v1/authentication/token",
    }

    local uri = ngx.var.uri
    for _, endpoint in ipairs(public_endpoints) do
        if uri == endpoint then
            return true
        end
    end
    return false
end

-- Hàm băm token bằng SHA-256
local function hash_token(token)
    local sha = sha256:new()
    sha:update(token)
    return str.to_hex(sha:final())
end

-- Hàm trả về lỗi 401 Unauthorized
local function unauthorized()
    ngx.status = 401
    ngx.say(cjson.encode({ code = 401, message = "Unauthorized", path = ngx.var.uri, timestamp = ngx.now() }))
    return ngx.exit(401)
end

-- Kết nối Redis để kiểm tra token
local function redis_connect()
    local red = redis:new()
    red:set_timeout(1000) -- 1 second timeout
    local ok, err = red:connect("redis", 6379)
    if not ok then
        ngx.log(ngx.ERR, "Failed to connect to Redis: ", err)
        return nil
    end
    local auth, err = red:auth("0000") -- Nếu Redis có password
    if not auth then
        ngx.log(ngx.ERR, "Failed to authenticate with Redis: ", err)
        return red
    end
    ngx.log(ngx.INFO, "Connected to Redis successfully")
    return red
end

local function get_token_from_header()
    local auth_header = ngx.req.get_headers()["Authorization"]
    if not auth_header then
        ngx.log(ngx.WARN, "No Authorization header found")
        return nil
    end
    -- Extract token from "Bearer <token>"
    local _, _, token = string.find(auth_header, "Bearer%s+(.+)")
    if not token then
        ngx.log(ngx.WARN, "Invalid Authorization header format")
        return nil
    end
    return token
end

local function get_access_from_redis(red, token_hash)
    -- Key format: "access:<hash>"
    local access_json, err = red:get("access:" .. token_hash)
    
    if err then
        ngx.log(ngx.ERR, "Redis get error: ", err)
        return nil
    end
    
    if not access_json or access_json == ngx.null then
        ngx.log(ngx.WARN, "Access token not found in Redis for hash: ", token_hash)
        return nil
    end
    
    -- Parse JSON
    local access_data = cjson.decode(access_json)
    if not access_data then
        ngx.log(ngx.ERR, "Failed to decode access JSON")
        return nil
    end
    
    -- Kiểm tra access token hết hạn chưa (expiresAt là milliseconds)
    local now_ms = ngx.now() * 1000
    if access_data.expiresAt and access_data.expiresAt < now_ms then
        ngx.log(ngx.WARN, "Access token expired in Redis")
        -- Xóa access token hết hạn
        red:del("access:" .. token_hash)
        return nil
    end
    
    ngx.log(ngx.INFO, "Access token found. AccountId: ", access_data.accountId)
    return access_data
end

local function sign_gateway_token(session_data)
    if not _G.PRIVATE_KEY then
        ngx.log(ngx.ERR, "Private key not loaded")
        return nil
    end
    
    local now = ngx.time()
    
    -- Payload chứa thông tin user từ Redis
    local payload = {
        -- Thông tin Gateway
        iss = "uniwise-gateway",  -- Issuer
        iat = now,
        exp = now + 300,  -- TTL: 5 phút
        
        -- Thông tin user từ Redis
        sub = session_data.accountId,           -- User ID
        jti = session_data.sessionId,           -- Session ID
        scope = session_data.scope,             -- Roles: "ROLE_USER ROLE_ADMIN"
        
        -- Metadata hữu ích
        client_ip = ngx.var.remote_addr,
        request_uri = ngx.var.uri,
        request_method = ngx.var.request_method
    }
    
    -- Headers JWT
    local headers = {
        typ = "JWT",
        alg = "RS256"
    }
    
    -- Ký token
    local token, err = jwt:sign(
        _G.PRIVATE_KEY,
        {
            header = headers,
            payload = payload
        }
    )
    
    if not token then
        ngx.log(ngx.ERR, "Failed to sign gateway token: ", err or "unknown")
        return nil
    end
    
    ngx.log(ngx.INFO, "Gateway token signed for user: ", session_data.accountId, 
            ", scope: ", session_data.scope, ", expires in 5 min")
    return token
end

local function main()
    -- STEP 1: Lấy token gốc từ Authorization header
    local auth_token = get_token_from_header()
    if not auth_token then
        ngx.log(ngx.WARN, "No auth token, continue without gateway token")
        -- Vẫn cho đi tiếp, service sẽ thấy không có X-Auth-Token
        return
    end
    
    -- STEP 2: Hash token để lookup trong Redis
    local token_hash = hash_token(auth_token)
    ngx.log(ngx.INFO, "Token hash: ", token_hash)
    
    -- STEP 3: Kết nối Redis
    local red = redis_connect()
    if not red then
        ngx.log(ngx.ERR, "Redis connection failed, continue without gateway token")
        return
    end
    
    -- STEP 4: Lấy access token từ Redis
    local session_data = get_access_from_redis(red, token_hash)
    
    -- Đóng kết nối Redis (dùng connection pool)
    local ok, err = red:set_keepalive(10000, 100)
    if not ok then
        ngx.log(ngx.WARN, "Failed to set Redis keepalive: ", err)
    end
    
    if not session_data then
        ngx.log(ngx.WARN, "No valid session found, continue without gateway token")
        return
    end
    
    -- STEP 5: Ký gateway token với thông tin user
    local gateway_token = sign_gateway_token(session_data)
    if not gateway_token then
        ngx.log(ngx.ERR, "Failed to sign gateway token")
        return
    end
    
    -- STEP 6: Gán token đã ký vào header
    ngx.req.set_header("X-Auth-Token", gateway_token)
    
    ngx.log(ngx.INFO, "Gateway token attached. User: ", session_data.accountId)
end

main()