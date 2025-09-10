package by.innowise.auth.service;

public interface RefreshTokenCleanupService {

    void clearTokenIfStored(String hashedToken);

}
