# first stage - build app preferably using common builder
FROM registry.promethist.ai/common/common/builder AS builder

WORKDIR /project

COPY . .

#TODO RUN some build command(s)

# second stage - build app runtime
FROM registry.promethist.ai/common/common/nginx

WORKDIR /app

#TODO copy app files from builder into runtime
COPY --from=builder /project/html /var/www/html

#TODO define app ENTRYPOINT