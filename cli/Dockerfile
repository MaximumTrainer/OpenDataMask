FROM golang:1.21-alpine AS build
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -o odm .

FROM alpine:latest
RUN apk --no-cache add ca-certificates
COPY --from=build /app/odm /usr/local/bin/odm
ENTRYPOINT ["odm"]
