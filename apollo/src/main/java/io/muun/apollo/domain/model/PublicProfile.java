package io.muun.apollo.domain.model;

import io.muun.apollo.domain.model.base.HoustonModel;
import io.muun.common.utils.Preconditions;

import android.support.annotation.Nullable;

import javax.validation.constraints.NotNull;

public class PublicProfile extends HoustonModel {

    @NotNull
    public final String firstName;

    @NotNull
    public final String lastName;

    @Nullable
    public final String profilePictureUrl;

    /**
     * Constructor.
     */
    public PublicProfile(
            @Nullable Long id,
            @NotNull Long hid,
            @NotNull String firstName,
            @NotNull String lastName,
            @Nullable String profilePictureUrl) {

        super(id, hid);
        this.firstName = firstName;
        this.lastName = lastName;
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getFullName() {
        //TODO: This format shouldn't be hardcoded here.
        return firstName + " " + lastName;
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Merge this PublicProfile with an updated copy, choosing whether to replace each field.
     */
    public PublicProfile mergeWithUpdate(PublicProfile other) {
        Preconditions.checkArgument(hid.equals(other.hid));
        Preconditions.checkArgument(other.id == null || id.equals(other.id));

        return new PublicProfile(
                id,
                hid,
                other.firstName,
                other.lastName,
                other.profilePictureUrl
        );
    }
}
