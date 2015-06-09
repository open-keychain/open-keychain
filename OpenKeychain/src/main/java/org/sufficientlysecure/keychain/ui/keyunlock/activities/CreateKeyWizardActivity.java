package org.sufficientlysecure.keychain.ui.keyunlock.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.keyunlock.Model.WizardModel;
import org.sufficientlysecure.keychain.ui.keyunlock.dialogs.PinUnlockDialog;
import org.sufficientlysecure.keychain.ui.keyunlock.dialogs.PinUnlockDialogViewModel;
import org.sufficientlysecure.keychain.ui.keyunlock.dialogs.UnlockDialog;
import org.sufficientlysecure.keychain.ui.keyunlock.wizard.EmailWizardFragment;
import org.sufficientlysecure.keychain.ui.keyunlock.wizard.NameWizardFragment;
import org.sufficientlysecure.keychain.ui.keyunlock.wizard.UnlockWizardFragment;
import org.sufficientlysecure.keychain.ui.keyunlock.wizard.WelcomeWizardFragment;
import org.sufficientlysecure.keychain.ui.keyunlock.wizard.WizardConfirmationFragment;

/**
 * Activity for creating keys with different security options.
 */
public class CreateKeyWizardActivity
        extends BaseActivity
        implements PinUnlockDialog.onKeyUnlockListener, WizardCommonListener {

    public static final String TAG = "CreateKeyWizardActivity";
    public static final String FRAGMENT_TAG = "CurrentWizardFragment";

    private CreateKeyWizardViewModel mCreateKeyWizardViewModel;
    private Button mNextButton;
    private Button mBackButton;
    private LinearLayout mCreateKeyWizardActivityButtonContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCreateKeyWizardViewModel = new CreateKeyWizardViewModel();
        mCreateKeyWizardViewModel.prepareViewModel(savedInstanceState, getIntent().getExtras(), this);

        if (savedInstanceState == null) {
            updateWizardState();
        }
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.create_key_wizard_activity);

        mNextButton = (Button) findViewById(R.id.nextButton);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNextClicked(v);
            }
        });

        mBackButton = (Button) findViewById(R.id.backButton);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackClicked(v);
            }
        });

        mCreateKeyWizardActivityButtonContainer =
                (LinearLayout) findViewById(R.id.createKeyWizardActivityButtonContainer);

        setTitle(getString(R.string.create_key_wizard_title));
    }

    /**
     * Updates the interface and the viewModel state when the user clicks on the back button.
     *
     * @param view
     */
    public void onBackClicked(View view) {
        getSupportFragmentManager().popBackStack();
        mCreateKeyWizardViewModel.updateWizardStateOnBack();
    }

    /**
     * Updates the interface and the viewModel state when the user clicks on the next button.
     *
     * @param view
     */
    public void onNextClicked(View view) {
        mCreateKeyWizardViewModel.updateWizardStateOnNext();
        updateWizardState();
    }

    /**
     * Callback for when the user confirms his unlock keyword (passphrase, pin, etc)
     * This method is only called when there is a double keyword confirmation.
     */
    @Override
    public void onNewUnlockKeywordConfirmed() {
        mCreateKeyWizardViewModel.updateWizardStateOnNext();
        updateWizardState();
    }

    /**
     * User canceled the unlock method dialog, go to previous step.
     */
    @Override
    public void onNewUnlockMethodCancel() {
        mCreateKeyWizardViewModel.updateWizardStateOnBack();
    }

    /**
     * Callback for when the user confirs his unlock pin
     * This method is only called when there is an unlock request.
     */
    @Override
    public void onUnlockRequest() {
        Log.v(TAG, "onUnlockRequest");
    }

    @Override
    public void onHideNavigationButtons(boolean hide) {
        if (hide) {
            mCreateKeyWizardActivityButtonContainer.setVisibility(View.INVISIBLE);
            mCreateKeyWizardActivityButtonContainer.animate().setDuration(400);
            mCreateKeyWizardActivityButtonContainer.animate().alpha(0);

        } else {
            mCreateKeyWizardActivityButtonContainer.setVisibility(View.VISIBLE);
            mCreateKeyWizardActivityButtonContainer.animate().setDuration(400);
            mCreateKeyWizardActivityButtonContainer.animate().alpha(1);
        }
    }

    @Override
    public void onAdvanceToNextWizardStep() {
        mCreateKeyWizardViewModel.updateWizardStateOnNext();
        updateWizardState();
    }

    @Override
    public WizardModel getModel() {
        return mCreateKeyWizardViewModel.getWizardModel();
    }

    /**
     * Updates the wizard screen state.
     */
    private void updateWizardState() {
        switch (mCreateKeyWizardViewModel.getWizardStep()) {
            case WIZARD_STEP_BEGIN: {
                WelcomeWizardFragment welcomeWizardFragment = WelcomeWizardFragment.newInstance();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.unlockWizardFragmentContainer, welcomeWizardFragment,
                        FRAGMENT_TAG);
                transaction.commit();
            }
            break;
            case WIZARD_STEP_CHOOSE_UNLOCK_METHOD: {
                mCreateKeyWizardActivityButtonContainer.setVisibility(View.VISIBLE);
                mCreateKeyWizardActivityButtonContainer.animate().setDuration(300);
                mCreateKeyWizardActivityButtonContainer.animate().alpha(1);

                UnlockWizardFragment unlockWizardFragment = UnlockWizardFragment.newInstance();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.addToBackStack(null);
                transaction.setCustomAnimations(R.anim.frag_slide_in_from_right,
                        R.anim.frag_slide_out_to_left,
                        R.anim.frag_slide_in_from_left, R.anim.frag_slide_out_to_right);
                transaction.replace(R.id.unlockWizardFragmentContainer, unlockWizardFragment,
                        FRAGMENT_TAG);
                transaction.commit();
            }
            break;
            case WIZARD_STEP_KEYWORD_INPUT_VERIFICATION: {
                PinUnlockDialog patternUnlockDialog = new PinUnlockDialog();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

                //set the operation
                Bundle bundle = new Bundle();
                bundle.putSerializable(UnlockDialog.EXTRA_PARAM_OPERATION_TYPE,
                        PinUnlockDialogViewModel.DialogUnlockOperation.
                                DIALOG_UNLOCK_TYPE_NEW_KEYWORD);

                patternUnlockDialog.setArguments(bundle);
                patternUnlockDialog.show(transaction, PinUnlockDialog.class.toString());
            }
            break;
            case WIZARD_STEP_CONTACT_NAME: {
                NameWizardFragment welcomeWizardFragment = NameWizardFragment.newInstance();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.frag_slide_in_from_right,
                        R.anim.frag_slide_out_to_left,
                        R.anim.frag_slide_in_from_left, R.anim.frag_slide_out_to_right);
                transaction.addToBackStack(null);
                transaction.replace(R.id.unlockWizardFragmentContainer, welcomeWizardFragment,
                        FRAGMENT_TAG);
                transaction.commit();

            }
            break;
            case WIZARD_STEP_CONTACT_EMAILS: {
                EmailWizardFragment welcomeWizardFragment = EmailWizardFragment.newInstance();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.frag_slide_in_from_right,
                        R.anim.frag_slide_out_to_left,
                        R.anim.frag_slide_in_from_left, R.anim.frag_slide_out_to_right);
                transaction.addToBackStack(null);
                transaction.replace(R.id.unlockWizardFragmentContainer, welcomeWizardFragment,
                        FRAGMENT_TAG);
                transaction.commit();
            }
            break;
            case WIZARD_STEP_FINALIZE: {
                //finalize the creation of the key
                WizardConfirmationFragment wizardConfirmationFragment = WizardConfirmationFragment.
                        newInstance();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.frag_slide_in_from_right,
                        R.anim.frag_slide_out_to_left,
                        R.anim.frag_slide_in_from_left, R.anim.frag_slide_out_to_right);
                transaction.addToBackStack(null);
                transaction.replace(R.id.unlockWizardFragmentContainer, wizardConfirmationFragment,
                        FRAGMENT_TAG);
                transaction.commit();
            }

            default:
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mCreateKeyWizardViewModel.saveViewModelState(outState);
    }
}
