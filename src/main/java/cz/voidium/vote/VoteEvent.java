package cz.voidium.vote;

public record VoteEvent(String serviceName, String username, String address, long timestamp) {
}
