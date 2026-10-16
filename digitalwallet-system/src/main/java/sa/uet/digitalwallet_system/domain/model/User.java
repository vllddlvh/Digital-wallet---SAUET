package sa.uet.digitalwallet_system.domain.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.sql.Timestamp;

@RequiredArgsConstructor
public class User {

    @NonNull
    int userId;

    @NonNull
    String userName;

    @NonNull
    String passWord;

    @NonNull
    String fullName;

    @NonNull
    String email;

    @NonNull
    String role;

    @NonNull
    Timestamp created_at;
}
