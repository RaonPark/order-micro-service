local balance = tonumber(redis.call("GET", KEYS[1]) or '0')
local amount = tonumber(ARGV[1])

if(balance < amount) then
    return balance
else
    redis.call('INCRBYFLOAT', KEYS[1], amount)
    return redis.call('GET', KEYS[1])
end