redis.call('SETNX', KEYS[1], 0)
return redis.call('INCR', KEYS[1])