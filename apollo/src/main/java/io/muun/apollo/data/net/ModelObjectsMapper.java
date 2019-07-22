package io.muun.apollo.data.net;

import io.muun.apollo.data.serialization.dates.ApolloZonedDateTime;
import io.muun.apollo.domain.errors.InvalidPhoneNumberError;
import io.muun.apollo.domain.model.BitcoinAmount;
import io.muun.apollo.domain.model.Contact;
import io.muun.apollo.domain.model.ExchangeRateWindow;
import io.muun.apollo.domain.model.FeeWindow;
import io.muun.apollo.domain.model.HardwareWallet;
import io.muun.apollo.domain.model.NextTransactionSize;
import io.muun.apollo.domain.model.NotificationReport;
import io.muun.apollo.domain.model.Operation;
import io.muun.apollo.domain.model.OperationCreated;
import io.muun.apollo.domain.model.PendingChallengeUpdate;
import io.muun.apollo.domain.model.PublicKeySet;
import io.muun.apollo.domain.model.PublicProfile;
import io.muun.apollo.domain.model.RealTimeData;
import io.muun.apollo.domain.model.SubmarineSwap;
import io.muun.apollo.domain.model.SubmarineSwapFundingOutput;
import io.muun.apollo.domain.model.SubmarineSwapReceiver;
import io.muun.apollo.domain.model.TransactionPushed;
import io.muun.apollo.domain.model.User;
import io.muun.apollo.domain.model.UserPhoneNumber;
import io.muun.apollo.domain.model.UserProfile;
import io.muun.common.Optional;
import io.muun.common.api.BitcoinAmountJson;
import io.muun.common.api.CommonModelObjectsMapper;
import io.muun.common.api.CreateSessionOkJson;
import io.muun.common.api.FeeWindowJson;
import io.muun.common.api.HardwareWalletJson;
import io.muun.common.api.HardwareWalletOutputJson;
import io.muun.common.api.HardwareWalletStateJson;
import io.muun.common.api.NextTransactionSizeJson;
import io.muun.common.api.OperationCreatedJson;
import io.muun.common.api.OperationJson;
import io.muun.common.api.PendingChallengeUpdateJson;
import io.muun.common.api.PhoneNumberJson;
import io.muun.common.api.PublicKeySetJson;
import io.muun.common.api.PublicProfileJson;
import io.muun.common.api.SizeForAmountJson;
import io.muun.common.api.SubmarineSwapFundingOutputJson;
import io.muun.common.api.SubmarineSwapJson;
import io.muun.common.api.SubmarineSwapReceiverJson;
import io.muun.common.api.TransactionPushedJson;
import io.muun.common.api.UserJson;
import io.muun.common.api.beam.notification.NotificationReportJson;
import io.muun.common.crypto.hd.HardwareWalletOutput;
import io.muun.common.crypto.hd.MuunAddress;
import io.muun.common.crypto.hd.PublicKeyPair;
import io.muun.common.crypto.hwallet.HardwareWalletState;
import io.muun.common.crypto.tx.PartiallySignedTransaction;
import io.muun.common.dates.MuunZonedDateTime;
import io.muun.common.model.CreateSessionOk;
import io.muun.common.model.SizeForAmount;
import io.muun.common.utils.CollectionUtils;
import io.muun.common.utils.Preconditions;

import android.support.annotation.NonNull;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bitcoinj.core.NetworkParameters;
import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;


@Singleton
public class ModelObjectsMapper extends CommonModelObjectsMapper {

    private final ObjectMapper jsonMapper;

    @Inject
    public ModelObjectsMapper(NetworkParameters networkParameters, ObjectMapper jsonMapper) {
        super(networkParameters);
        this.jsonMapper = jsonMapper;
    }

    /**
     * Create a date time.
     */
    @Nullable
    private ZonedDateTime mapZonedDateTime(@Nullable MuunZonedDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return ((ApolloZonedDateTime) dateTime).dateTime;
    }

    /**
     * Create a user.
     */
    @NotNull
    public User mapUser(@NotNull UserJson apiUser) {

        final Optional<UserProfile> maybeProfile = Optional.ofNullable(apiUser.publicProfile)
                .map(this::mapUserProfile);

        final Optional<UserPhoneNumber> maybePhoneNumber = Optional.ofNullable(apiUser.phoneNumber)
                .map(this::mapUserPhoneNumber);

        return new User(
                apiUser.id,
                apiUser.email,
                apiUser.isEmailVerified,
                maybePhoneNumber,
                maybeProfile,
                apiUser.primaryCurrency,
                apiUser.hasRecoveryCodeChallengeKey,
                apiUser.hasP2PEnabled
        );
    }

    /**
     * Create a public profile.
     */
    @NotNull
    private PublicProfile mapPublicProfile(@NotNull PublicProfileJson publicProfile) {

        return new PublicProfile(
                null,
                publicProfile.userId,
                publicProfile.firstName,
                publicProfile.lastName,
                publicProfile.profilePictureUrl
        );
    }

    public UserProfile mapUserProfile(@NotNull PublicProfileJson profile) {
        return new UserProfile(profile.firstName, profile.lastName, profile.profilePictureUrl);
    }

    /**
     * Create a UserPhoneNumber.
     */
    public UserPhoneNumber mapUserPhoneNumber(@NotNull PhoneNumberJson phoneNumber) {
        try {
            return new UserPhoneNumber(phoneNumber.number, phoneNumber.isVerified);
        } catch (IllegalArgumentException e) {
            throw new InvalidPhoneNumberError(e);
        }
    }

    /**
     * Create a contact.
     */
    @NotNull
    public Contact mapContact(@NotNull io.muun.common.api.Contact contact) {

        return new Contact(
                null,
                contact.publicProfile.userId,
                mapPublicProfile(contact.publicProfile),
                contact.maxAddressVersion,
                mapPublicKey(contact.publicKey),
                mapPublicKey(contact.cosigningPublicKey),
                contact.lastDerivationIndex
        );
    }

    /**
     * Create an exchange rate window.
     */
    @NotNull
    private ExchangeRateWindow mapExchangeRateWindow(
            @NotNull io.muun.common.api.ExchangeRateWindow window) {

        return new ExchangeRateWindow(
                window.id,
                mapZonedDateTime(window.fetchDate),
                window.rates
        );
    }

    /**
     * Create a bitcoin amount.
     */
    @NotNull
    private BitcoinAmount mapBitcoinAmount(@NotNull BitcoinAmountJson bitcoinAmount) {

        return new BitcoinAmount(
                bitcoinAmount.inSatoshis,
                bitcoinAmount.inInputCurrency,
                bitcoinAmount.inPrimaryCurrency
        );
    }

    /**
     * Create an operation.
     */
    @NotNull
    public Operation mapOperation(@NotNull OperationJson operation) {

        Preconditions.checkNotNull(operation.id);

        return new Operation(
                null,
                operation.id,
                operation.direction,
                operation.isExternal,
                operation.senderProfile != null ? mapPublicProfile(operation.senderProfile) : null,
                operation.senderIsExternal,
                operation.receiverProfile != null
                        ? mapPublicProfile(operation.receiverProfile) : null,
                operation.receiverIsExternal,
                operation.receiverAddress,
                operation.receiverAddressDerivationPath,
                operation.hardwareWalletHid,
                mapBitcoinAmount(operation.amount),
                mapBitcoinAmount(operation.fee),
                operation.transaction != null ? operation.transaction.confirmations : 0L,
                operation.transaction != null ? operation.transaction.hash : null,
                operation.description,
                operation.status,
                mapZonedDateTime(operation.creationDate),
                operation.exchangeRatesWindowId,
                operation.swap != null ? mapSubmarineSwap(operation.swap) : null
        );
    }

    /**
     * Create an operation swap.
     */
    @NotNull
    public SubmarineSwap mapSubmarineSwap(SubmarineSwapJson swap) {
        if (swap == null) {
            return null;
        }

        return new SubmarineSwap(
                null,
                swap.swapUuid,
                swap.invoice,
                mapSwapReceiver(swap.receiver),
                mapSwapFundingOutput(swap.fundingOutput),
                swap.sweepFeeInSatoshis,
                swap.lightningFeeInSatoshis,
                mapZonedDateTime(swap.expiresAt),
                mapZonedDateTime(swap.payedAt),
                swap.preimageInHex
        );
    }

    @NonNull
    private SubmarineSwapFundingOutput mapSwapFundingOutput(SubmarineSwapFundingOutputJson output) {

        return new SubmarineSwapFundingOutput(
                output.outputAddress,
                output.outputAmountInSatoshis,
                output.confirmationsNeeded,
                output.userLockTime,
                MuunAddress.fromJson(output.userRefundAddress),
                output.serverPaymentHashInHex,
                output.serverPublicKeyInHex
        );
    }

    @NotNull
    private SubmarineSwapReceiver mapSwapReceiver(@NotNull SubmarineSwapReceiverJson receiver) {

        try {

            return new SubmarineSwapReceiver(
                    receiver.alias,
                    jsonMapper.writeValueAsString(receiver.networkAddresses),
                    receiver.publicKey
            );

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a partially signed transaction.
     */
    @NotNull
    public OperationCreated mapOperationCreated(@NotNull OperationCreatedJson operationCreated) {
        final OperationJson apiOperation = operationCreated.operation;

        Preconditions.checkNotNull(operationCreated.operation);
        Preconditions.checkNotNull(operationCreated.nextTransactionSize);

        final Operation operation = mapOperation(apiOperation);

        return new OperationCreated(
                operation,
                PartiallySignedTransaction.fromJson(
                        operationCreated.partiallySignedTransaction,
                        networkParameters
                ),
                mapNextTransactionSize(operationCreated.nextTransactionSize)
        );
    }

    /**
     * Create a TransactionPushed object.
     */
    @NotNull
    public TransactionPushed mapTransactionPushed(@NotNull TransactionPushedJson txPushed) {
        Preconditions.checkNotNull(txPushed.hex);
        Preconditions.checkNotNull(txPushed.nextTransactionSize);

        return new TransactionPushed(
                txPushed.hex,
                mapNextTransactionSize(txPushed.nextTransactionSize)
        );
    }

    /**
     * Create an expected fee.
     */
    @NotNull
    private FeeWindow mapFeeWindow(@NotNull FeeWindowJson window) {

        return new FeeWindow(
                window.id,
                mapZonedDateTime(window.fetchDate),
                window.targetedFees
        );
    }

    /**
     * Create a bag of real-time data provided by Houston.
     */
    @NotNull
    public RealTimeData mapRealTimeData(@NotNull io.muun.common.api.RealTimeData realTimeData) {
        return new RealTimeData(
                mapFeeWindow(realTimeData.feeWindow),
                mapExchangeRateWindow(realTimeData.exchangeRateWindow)
        );
    }

    /**
     * Create a NotificationReport.
     */
    @NotNull
    public NotificationReport mapNotificationReport(@NotNull NotificationReportJson reportJson) {
        return new NotificationReport(
                reportJson.previousId,
                reportJson.maximumId,
                reportJson.preview
        );
    }

    /**
     * Create a NextTransactionSize.
     */
    @NotNull
    public NextTransactionSize mapNextTransactionSize(@NotNull NextTransactionSizeJson json) {

        final ArrayList<SizeForAmount> progression = new ArrayList<>(json.sizeProgression.size());

        for (SizeForAmountJson sizeForAmount : json.sizeProgression) {
            progression.add(mapSizeForAmount(sizeForAmount));
        }

        return new NextTransactionSize(
                progression,
                json.validAtOperationHid
        );
    }

    /**
     * Create a SizeForAmount.
     */
    @NotNull
    private SizeForAmount mapSizeForAmount(@NotNull SizeForAmountJson sizeForAmount) {

        return new SizeForAmount(
                sizeForAmount.amountInSatoshis,
                sizeForAmount.sizeInBytes.intValue()
        );
    }

    /**
     * Create a PublicKeySet.
     */
    @Nullable
    public PublicKeySet mapPublicKeySet(PublicKeySetJson publicKeySet) {
        return new PublicKeySet(
                new PublicKeyPair(
                        mapPublicKey(publicKeySet.basePublicKey),
                        mapPublicKey(publicKeySet.baseCosigningPublicKey)
                ),
                publicKeySet.externalPublicKeyIndices.maxUsedIndex,
                publicKeySet.externalPublicKeyIndices.maxWatchingIndex
        );
    }

    public PendingChallengeUpdate mapPendingChallengeUpdate(PendingChallengeUpdateJson json) {
        return new PendingChallengeUpdate(json.uuid, json.type);
    }

    /**
     * Create a CreateSessionOk.
     */
    public CreateSessionOk mapCreateSessionOk(CreateSessionOkJson json) {
        return new CreateSessionOk(
            json.isExistingUser,
            json.canUseRecoveryCode
        );
    }

    /**
     * Create a HardwareWallet.
     */
    public HardwareWallet mapHardwareWallet(HardwareWalletJson json) {
        // NOTE: despite safeguards, `id` and `createdAt` are always non-null coming from Houston.
        return new HardwareWallet(
                null,
                json.id,
                json.brand,
                json.model,
                json.label,
                mapPublicKey(json.publicKey),
                mapZonedDateTime(json.createdAt),
                mapZonedDateTime(json.lastPairedAt),
                json.isPaired
        );
    }

    /**
     * Create an HardwareWalletOutput.
     */
    private HardwareWalletOutput mapHardwareWalletOutput(HardwareWalletOutputJson json) {
        return new HardwareWalletOutput(
                json.txId,
                json.index,
                json.amount,
                mapPublicKey(json.publicKeyJson),
                json.rawPreviousTransaction
        );
    }

    /**
     * Create an HardwareWalletStateJson.
     */
    public HardwareWalletState mapHardwareWalletState(HardwareWalletStateJson json) {

        return new HardwareWalletState(
                CollectionUtils.mapList(json.sortedUtxos, this::mapHardwareWalletOutput),
                CollectionUtils.mapList(json.sizeForAmount, this::mapSizeForAmount),
                mapHardwareWalletAddress(json.changeAddress),
                mapHardwareWalletAddress(json.nextAddress)
         );
    }
}