-- Atomic sliding-window rate limiter using Redis Sorted Sets (ZSET)
-- KEYS[1]: rate limit key (e.g. rl:create:ip:192.0.2.1)
-- ARGV[1]: current timestamp in epoch milliseconds
-- ARGV[2]: window duration in milliseconds
-- ARGV[3]: maximum allowed requests in window
-- ARGV[4]: unique request member ID

local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local member = ARGV[4]

-- Step 1: Evict entries outside the sliding window
local clearBefore = now - window
redis.call('ZREMRANGEBYSCORE', key, '-inf', clearBefore)

-- Step 2: Check current count of requests within the active window
local currentCount = redis.call('ZCARD', key)

if currentCount < limit then
    -- Allowed: record this request timestamp
    redis.call('ZADD', key, now, member)
    -- Keep key alive for the window duration plus a buffer
    local ttlSeconds = math.ceil(window / 1000) + 1
    redis.call('EXPIRE', key, ttlSeconds)
    local remaining = limit - currentCount - 1
    return {1, remaining, 0}
else
    -- Rejected: calculate when the oldest request rolls out of the window
    local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
    local retryAfter = 1
    if #oldest > 0 then
        local oldestScore = tonumber(oldest[2])
        retryAfter = math.ceil((oldestScore + window - now) / 1000)
        if retryAfter < 1 then
            retryAfter = 1
        end
    end
    return {0, 0, retryAfter}
end
