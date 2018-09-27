package io.muun.apollo.data.preferences;

import io.muun.common.Optional;

import android.content.Context;
import com.f2prateek.rx.preferences.Preference;

import javax.inject.Inject;

public class ClientVersionRepository extends BaseRepository {

    private static final String KEY_MIN_CLIENT_VERSION = "min_client_version";

    private final Preference<Integer> minClientVersionPreference;

    /**
     * Creates a repository for auth data.
     */
    @Inject
    public ClientVersionRepository(Context context) {
        super(context);
        minClientVersionPreference = rxSharedPreferences.getInteger(KEY_MIN_CLIENT_VERSION);
    }

    @Override
    protected String getFileName() {
        return "clientVersion";
    }

    /**
     * Save minClientVersion in preferences.
     */
    public void storeMinClientVersion(int minClientVersion) {
        this.minClientVersionPreference.set(minClientVersion);
    }

    /**
     * Load minClientVersion from preferences, if present.
     */
    public Optional<Integer> getMinClientVersion() {
        if (minClientVersionPreference.isSet()) {
            return Optional.of(minClientVersionPreference.get());
        } else {
            return Optional.empty();
        }
    }
}
