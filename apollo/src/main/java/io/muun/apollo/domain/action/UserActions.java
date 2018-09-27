package io.muun.apollo.domain.action;

import io.muun.apollo.data.net.HoustonClient;
import io.muun.apollo.data.preferences.AuthRepository;
import io.muun.apollo.data.preferences.KeysRepository;
import io.muun.apollo.data.preferences.UserRepository;
import io.muun.apollo.domain.action.base.AsyncAction0;
import io.muun.apollo.domain.action.base.AsyncAction1;
import io.muun.apollo.domain.action.base.AsyncAction2;
import io.muun.apollo.domain.action.base.AsyncActionStore;
import io.muun.apollo.domain.model.ContactsPermissionState;
import io.muun.apollo.domain.model.PendingChallengeUpdate;
import io.muun.apollo.domain.model.User;
import io.muun.apollo.domain.model.UserPhoneNumber;
import io.muun.apollo.domain.model.UserProfile;
import io.muun.common.api.SetupChallengeResponse;
import io.muun.common.crypto.ChallengePrivateKey;
import io.muun.common.crypto.ChallengePublicKey;
import io.muun.common.crypto.ChallengeType;
import io.muun.common.model.PhoneNumber;
import io.muun.common.model.VerificationType;
import io.muun.common.model.challenge.Challenge;
import io.muun.common.model.challenge.ChallengeSetup;
import io.muun.common.model.challenge.ChallengeSignature;
import io.muun.common.rx.RxHelper;
import io.muun.common.utils.RandomGenerator;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import rx.Observable;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.money.CurrencyUnit;


@Singleton
public class UserActions {

    private final UserRepository userRepository;
    private final KeysRepository keysRepository;
    private final AuthRepository authRepository;

    private final HoustonClient houstonClient;

    private final AddressActions addressActions;
    private final ContactActions contactActions;

    public final AsyncAction1<PhoneNumber, UserPhoneNumber> createPhoneAction;
    public final AsyncAction1<VerificationType, Void> resendVerificationCodeAction;
    public final AsyncAction1<String, UserPhoneNumber> confirmPhoneAction;
    public final AsyncAction1<UserProfile, UserProfile> createProfileAction;

    public final AsyncAction2<String, String, User> updateUsernameAction;
    public final AsyncAction0<UserProfile> updateProfilePictureAction;
    public final AsyncAction1<CurrencyUnit, User> updatePrimaryCurrencyAction;

    public final AsyncAction2<String, ChallengeType, PendingChallengeUpdate>
            beginPasswordChangeAction;
    public final AsyncAction2<String, String, Void> finishPasswordChangeAction;

    public final AsyncAction1<String, Void> submitFeedbackAction;

    /**
     * Constructor.
     */
    @Inject
    public UserActions(AsyncActionStore asyncActionStore,
                       UserRepository userRepository,
                       KeysRepository keysRepository,
                       AuthRepository authRepository,
                       HoustonClient houstonClient,
                       AddressActions addressActions,
                       ContactActions contactActions) {

        this.userRepository = userRepository;
        this.keysRepository = keysRepository;
        this.authRepository = authRepository;
        this.houstonClient = houstonClient;

        this.addressActions = addressActions;
        this.contactActions = contactActions;

        this.createPhoneAction = asyncActionStore
            .get("user/createPhone", this::createPhone);

        this.resendVerificationCodeAction = asyncActionStore
            .get("user/resendCode", houstonClient::resendVerificationCode);

        this.confirmPhoneAction = asyncActionStore
            .get("user/confirm-phone", this::confirmPhone);

        this.createProfileAction = asyncActionStore
            .get("user/createProfile", this::createProfile);


        this.updateUsernameAction = asyncActionStore
            .get("user/editUsername", this::updateUsername);

        this.updateProfilePictureAction = asyncActionStore
            .get("user/editProfilePicture", this::uploadPendingProfilePictureIfPresent);

        this.updatePrimaryCurrencyAction = asyncActionStore
            .get("user/editPrimaryCurrency", this::updatePrimaryCurrency);

        this.beginPasswordChangeAction = asyncActionStore
            .get("user/beginChangePassword", this::beginPasswordChange);

        this.finishPasswordChangeAction = asyncActionStore
            .get("user/finishChangePassword", this::finishPasswordChange);

        this.submitFeedbackAction = asyncActionStore
            .get("user/submitFeedbackAction", this::submitFeedback);
    }

    public User fetchOneUser() {
        return userRepository.fetchOne();
    }

    public Observable<User> fetchUser() {
        return userRepository.fetch();
    }

    public void setPendingProfilePicture(@Nullable Uri uri) {
        userRepository.setPendingProfilePictureUri(uri);
    }

    /**
     * Uploads the pending profile picture if present.
     */
    public Observable<UserProfile> uploadPendingProfilePictureIfPresent() {

        final Uri pendingProfilePictureUri = userRepository.getPendingProfilePictureUri();

        if (pendingProfilePictureUri == null) {
            return Observable.just(null);
        }

        return houstonClient.uploadProfilePicture(pendingProfilePictureUri)
            .doOnNext(userRepository::storeProfile)
            .doOnNext(ignore -> userRepository.setPendingProfilePictureUri(null));
    }

    /**
     * Updates the user status when the email gets verified.
     */
    public void verifyEmail() {
        userRepository.storeEmailVerified();
    }

    public Observable<Boolean> watchForEmailVerification() {
        return userRepository.fetch().map(user -> user.isEmailVerified);
    }

    /**
     * Creates the user phone number.
     */
    private Observable<UserPhoneNumber> createPhone(PhoneNumber phoneNumber) {
        return houstonClient.createPhone(phoneNumber)
                .doOnNext(userRepository::storePhoneNumber);
    }

    /**
     * Confirm the user phone number.
     */
    private Observable<UserPhoneNumber> confirmPhone(String verificationCode) {
        return houstonClient.confirmPhone(verificationCode)
                .doOnNext(userRepository::storePhoneNumber);
    }

    /**
     * Creates the user profile.
     */
    private Observable<UserProfile> createProfile(UserProfile userProfile) {
        return houstonClient.createProfile(userProfile)
                .doOnNext(userRepository::store)
                .flatMap(ignore -> uploadPendingProfilePictureIfPresent());
    }

    private Observable<User> updateUsername(String firstName, String lastName) {
        return houstonClient.updateUsername(firstName, lastName)
                .doOnNext(userRepository::store);
    }

    private Observable<User> updatePrimaryCurrency(CurrencyUnit currencyUnit) {
        return houstonClient.updatePrimaryCurrency(currencyUnit)
                .doOnNext(userRepository::store);
    }

    public Observable<String> getEncryptedMuunPrivateKey() {
        return keysRepository.getEncryptedMuunPrivateKey();
    }

    public Observable<String> getEncryptedBasePrivateKey() {
        return keysRepository.getEncryptedBasePrivateKey();
    }

    public Observable<Boolean> watchHasRecoveryCode() {
        return userRepository.watchHasRecoveryCode();
    }

    public boolean hasRecoveryCode() {
        return userRepository.hasRecoveryCode();
    }

    /**
     * Starts password change process by requesting a challenge and sending a challenge signature,
     * signed with the current password or recovery code.
     */
    public Observable<PendingChallengeUpdate> beginPasswordChange(String userInput,
                                                                  ChallengeType challengeType) {

        return houstonClient.requestChallenge(challengeType)
            .flatMap(maybeChallenge -> {
                if (!maybeChallenge.isPresent()) {
                    // TODO ???
                }

                final byte[] signature = signChallenge(userInput, maybeChallenge.get());

                return houstonClient.beginPasswordChange(
                    new ChallengeSignature(challengeType, signature)
                );
            });
    }

    public Observable<String> awaitPasswordChangeEmailAuthorization() {
        return userRepository.awaitForAuthorizedPasswordChange();
    }

    public void authorizePasswordChange(String uuid) {
        userRepository.storePasswordChangeStatus(uuid);
    }

    /**
     * Finish a password change process by submitting a new ChallengeSetup, built with the
     * new password, and a process' identifying uuid.
     */
    private Observable<Void> finishPasswordChange(String uuid, String password) {
        return buildChallengeSetup(ChallengeType.PASSWORD, password)
                .flatMap(setupChallenge ->
                        houstonClient.finishPasswordChange(uuid, setupChallenge),
                        Pair::new
                )
                .doOnNext(pair -> {
                    final ChallengeSetup challengeSetup = pair.first;
                    final SetupChallengeResponse setupChallengeResponse = pair.second;

                    if (setupChallengeResponse.muunKey !=  null) {
                        keysRepository.storeEncryptedMuunPrivateKey(setupChallengeResponse.muunKey);
                    }

                    storeChallengeKey(ChallengeType.PASSWORD, challengeSetup.publicKey);
                })
                .map(RxHelper::toVoid);
    }

    /**
     * Build a ChallengeSetup of specified ChallengeType using userInput.
     *
     * @see ChallengeSetup
     * @see ChallengeType
     */
    @NonNull
    public Observable<ChallengeSetup> buildChallengeSetup(ChallengeType challengeType,
                                                          String userInput) {
        final byte[] salt = generateSaltForChallengeKey();

        final ChallengePublicKey publicKey = ChallengePrivateKey.fromUserInput(userInput, salt)
                .getChallengePublicKey();

        final Observable<String> afterEncryptingPrivateKey = keysRepository
                .getBasePrivateKey()
                .map(key -> addressActions.encryptRootPrivateKey(key, userInput));

        return afterEncryptingPrivateKey.map(encryptedPrivateKey -> new ChallengeSetup(
            challengeType,
            publicKey,
            salt,
            encryptedPrivateKey,
            ChallengeType.getVersion(challengeType)
        ));
    }

    /**
     * Store a Challenge PublicKey, and if it's the RecoveryCode Challenge PublicKey, update
     * preferences accordingly.
     */
    public void storeChallengeKey(ChallengeType challengeType, ChallengePublicKey publicKey) {

        if (challengeType.equals(ChallengeType.RECOVERY_CODE)) {
            userRepository.storeHasRecoveryCode(true);
        }

        keysRepository.storePublicChallengeKey(publicKey, challengeType);
    }

    public byte[] generateSaltForChallengeKey() {
        return RandomGenerator.getBytes(8);
    }

    // This method is duplicated. (Also in SigninActions)
    private byte[] signChallenge(String userInput, Challenge challenge) {

        final ChallengePrivateKey challengePrivateKey = ChallengePrivateKey.fromUserInput(
                userInput,
                challenge.salt
        );

        return challengePrivateKey.sign(challenge.challenge);
    }

    private Observable<Void> submitFeedback(String feedbackContent) {
        return houstonClient.submitFeedback(feedbackContent);
    }

    public void resetPhoneNumber() {
        userRepository.storePhoneNumber(null);
    }

    public void reportContactsPermissionNeverAskAgain() {
        userRepository.storeContactsPermissionState(ContactsPermissionState.PERMANENTLY_DENIED);
    }

    public Observable<ContactsPermissionState> watchContactsPermissionState() {
        return userRepository.watchContactsPermissionState();
    }

    /**
     * Update value of user preference tracking Contacts permission state.
     * This receives as param the result of asking if permission is granted, that's why it is a
     * boolean: true for GRANTED, false for DENIED.
     *
     * <p>Helpful: table of values
     *
     * <p>if current_state is GRANTED           && new_value is GRANTED =>  GRANTED
     * if current_state is GRANTED              && new_value is DENIED  =>  DENIED
     * if current_state is DENIED               && new_value is GRANTED =>  GRANTED
     * if current_state is DENIED               && new_value is DENIED  =>  DENIED
     * if current_state is PERMANENTLY_DENIED   && new_value is GRANTED =>  GRANTED
     * if current_state is PERMANENTLY_DENIED   && new_value is DENIED  =>  PERMANENTLY_DENIED
     */
    public void updateContactsPermissionState(boolean granted) {

        final ContactsPermissionState prevState = userRepository.getContactsPermissionState();

        if (granted) {
            // if we detect a permission grant (via android settings) => trigger sync phone contacts
            if (authRepository.isLoggedIn() && prevState != ContactsPermissionState.GRANTED) {
                contactActions.initialSyncPhoneContactsAction.run();
            }

            userRepository.storeContactsPermissionState(ContactsPermissionState.GRANTED);
            return;
        }

        if (prevState != ContactsPermissionState.PERMANENTLY_DENIED) {
            userRepository.storeContactsPermissionState(ContactsPermissionState.DENIED);
        }
    }
}
