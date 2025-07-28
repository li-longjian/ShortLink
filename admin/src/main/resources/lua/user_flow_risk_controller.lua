local username = KEYS[1]
local timeWindow = tonumber(ARGV[1]) -- 时间窗口，单位：秒
-- 构造 Redis 中存储用户访问次数的键名
local accessKey = "shortlink:user-flow-risk-control:" .. username
-- 检查键是否存在
local keyExists = redis.call("EXISTS", accessKey)
-- 若键已存在，原子递增访问次数并返回递增后的值
if keyExists == 1 then
    local currentAccessCount = redis.call("INCR", accessKey)
return currentAccessCount
else    -- 若键不存在，创建键并设置过期时间，然后原子递增访问次数并返回递增后的值
    redis.call("SET", accessKey, 1, "EX", timeWindow)
    local currentAccessCount = redis.call("INCR", accessKey)
    return currentAccessCount
end