import time

value = False

while True:
    print(value, flush=True)
    value = not value
    time.sleep(5.0)
