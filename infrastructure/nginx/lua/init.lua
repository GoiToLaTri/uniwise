local function read_file(path)
    local f = io.open(path, "rb")
    if not f then 
        ngx.log(ngx.ERR, "Cannot open file: ", path)
        return nil 
    end
    local content = f:read("*all")
    f:close()
    return content
end

-- Load keys
_G.PRIVATE_KEY = read_file("/usr/local/openresty/nginx/cert/private.pem")
_G.PUBLIC_KEY = read_file("/usr/local/openresty/nginx/cert/public.pem")

if not _G.PRIVATE_KEY then
    ngx.log(ngx.ERR, "FATAL: Could not load PRIVATE_KEY")
end
if not _G.PUBLIC_KEY then
    ngx.log(ngx.ERR, "FATAL: Could not load PUBLIC_KEY")
end

ngx.log(ngx.INFO, "--- Keys loaded ---")
ngx.log(ngx.INFO, "Private key: ", _G.PRIVATE_KEY and "LOADED" or "MISSING")
ngx.log(ngx.INFO, "Public key: ", _G.PUBLIC_KEY and "LOADED" or "MISSING")