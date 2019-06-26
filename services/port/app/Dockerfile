# first stage - build app preferably using common builder
FROM registry.gitlab.com/promethistai/common/builder as BUILDER

WORKDIR /project

COPY . .

#TODO RUN some build command(s)

# second stage - build app runtime
FROM registry.gitlab.com/promethistai/common/nginx

WORKDIR /app

#TODO copy app files from builder into runtime
COPY --from=builder /project/www /var/www/html

#TODO define app ENTRYPOINT