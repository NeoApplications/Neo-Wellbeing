package org.eu.droid_ng.wellbeing;

import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/* This file contains all references to private API. Private API will not be used elsewhere */
public class PackageManagerDelegate {
	private final PackageManager pm;

	public PackageManagerDelegate(PackageManager pm) {
		this.pm = pm;
	}

	public String[] setPackagesSuspended(@Nullable String[] packageNames, boolean suspend, @Nullable PersistableBundle appExtras, @Nullable PersistableBundle launcherExtras, @Nullable SuspendDialogInfo dialogInfo) {
		return pm.setPackagesSuspended(packageNames, suspend, appExtras, launcherExtras, dialogInfo == null ? null : dialogInfo.real);
	}

	public static class SuspendDialogInfo {
		public android.content.pm.SuspendDialogInfo real;

		/**
		 * Used with {@link Builder#setNeutralButtonAction(int)} to create a neutral button that
		 * starts the Intent#ACTION_SHOW_SUSPENDED_APP_DETAILS activity.
		 * @see Builder#setNeutralButtonAction(int)
		 */
		public static final int BUTTON_ACTION_MORE_DETAILS = 0;

		/**
		 * Used with {@link Builder#setNeutralButtonAction(int)} to create a neutral button that
		 * unsuspends the app that the user was trying to launch and continues with the launch. The
		 * system also sends the broadcast
		 * Intent#ACTION_PACKAGE_UNSUSPENDED_MANUALLY to the suspending app
		 * when this happens.
		 * @see Builder#setNeutralButtonAction(int)
		 * see ACTION_PACKAGE_UNSUSPENDED_MANUALLY
		 */
		public static final int BUTTON_ACTION_UNSUSPEND = 1;

		/**
		 * Button actions to specify what happens when the user taps on the neutral button.
		 * To be used with {@link Builder#setNeutralButtonAction(int)}.
		 *
		 * @hide
		 * @see Builder#setNeutralButtonAction(int)
		 */
		@IntDef(flag = true, value = {
				BUTTON_ACTION_MORE_DETAILS,
				BUTTON_ACTION_UNSUSPEND
		})
		@Retention(RetentionPolicy.SOURCE)
		public @interface ButtonAction {
		}

		/**
		 * @return the resource id of the icon to be used with the dialog
		 * @hide
		 */
		@DrawableRes
		public int getIconResId() {
			return real.getIconResId();
		}

		/**
		 * @return the resource id of the title to be used with the dialog
		 * @hide
		 */
		@StringRes
		public int getTitleResId() {
			return real.getTitleResId();
		}

		/**
		 * @return the resource id of the text to be shown in the dialog's body
		 * @hide
		 */
		@StringRes
		public int getDialogMessageResId() {
			return real.getDialogMessageResId();
		}

		/**
		 * @return the text to be shown in the dialog's body. Returns {@code null} if {@link
		 * #getDialogMessageResId()} returns a valid resource id
		 * @hide
		 */
		@Nullable
		public String getDialogMessage() {
			return real.getDialogMessage();
		}

		/**
		 * @return the text to be shown
		 * @hide
		 */
		@StringRes
		public int getNeutralButtonTextResId() {
			return real.getNeutralButtonTextResId();
		}

		/**
		 * @return The {@link ButtonAction} that happens on tapping this button
		 */
		@ButtonAction
		public int getNeutralButtonAction() {
			return real.getNeutralButtonAction();
		}

		@Override
		public int hashCode() {
			return real.hashCode();
		}

		public SuspendDialogInfo(Builder b) {
			real = b.realb.build();
		}

		private SuspendDialogInfo(android.content.pm.SuspendDialogInfo r) {
			real = r;
		}

		/**
		 * Builder to build a {@link SuspendDialogInfo} object.
		 */
		public static final class Builder {
			public android.content.pm.SuspendDialogInfo.Builder realb = new android.content.pm.SuspendDialogInfo.Builder();
			/**
			 * Set the resource id of the icon to be used. If not provided, no icon will be shown.
			 *
			 * @param resId The resource id of the icon.
			 * @return this builder object.
			 */
			@NonNull
			public Builder setIcon(@DrawableRes int resId) {
				realb.setIcon(resId);
				return this;
			}

			/**
			 * Set the resource id of the title text to be displayed. If this is not provided, the
			 * system will use a default title.
			 *
			 * @param resId The resource id of the title.
			 * @return this builder object.
			 */
			@NonNull
			public Builder setTitle(@StringRes int resId) {
				realb.setTitle(resId);
				return this;
			}

			/**
			 * Set the text to show in the body of the dialog. Ignored if a resource id is set via
			 * {@link #setMessage(int)}.
			 * <p>
			 * The system will use String#format(Locale, String, Object...) to
			 * insert the suspended app name into the message, so an example format string could be
			 * {@code "The app %1$s is currently suspended"}. This is optional - if the string passed in
			 * {@code message} does not accept an argument, it will be used as is.
			 *
			 * @param message The dialog message.
			 * @return this builder object.
			 * @see #setMessage(int)
			 */
			@NonNull
			public Builder setMessage(@NonNull String message) {
				realb.setMessage(message);
				return this;
			}

			/**
			 * Set the resource id of the dialog message to be shown. If no dialog message is provided
			 * via either this method or {@link #setMessage(String)}, the system will use a default
			 * message.
			 * <p>
			 * The system will use {@link android.content.res.Resources#getString(int, Object...)
			 * getString} to insert the suspended app name into the message, so an example format string
			 * could be {@code "The app %1$s is currently suspended"}. This is optional - if the string
			 * referred to by {@code resId} does not accept an argument, it will be used as is.
			 *
			 * @param resId The resource id of the dialog message.
			 * @return this builder object.
			 * @see #setMessage(String)
			 */
			@NonNull
			public Builder setMessage(@StringRes int resId) {
				realb.setMessage(resId);
				return this;
			}

			/**
			 * Set the resource id of text to be shown on the neutral button. Tapping this button would
			 * perform the {@link ButtonAction action} specified through
			 * {@link #setNeutralButtonAction(int)}. If this is not provided, the system will use a
			 * default text.
			 *
			 * @param resId The resource id of the button text
			 * @return this builder object.
			 */
			@NonNull
			public Builder setNeutralButtonText(@StringRes int resId) {
				realb.setNeutralButtonText(resId);
				return this;
			}

			/**
			 * Set the action expected to happen on neutral button tap. Defaults to
			 * {@link #BUTTON_ACTION_MORE_DETAILS} if this is not provided.
			 *
			 * @param buttonAction Either {@link #BUTTON_ACTION_MORE_DETAILS} or
			 *                     {@link #BUTTON_ACTION_UNSUSPEND}.
			 * @return this builder object
			 */
			@NonNull
			public Builder setNeutralButtonAction(@ButtonAction int buttonAction) {
				realb.setNeutralButtonAction(buttonAction);
				return this;
			}

			/**
			 * Build the final object based on given inputs.
			 *
			 * @return The {@link SuspendDialogInfo} object built using this builder.
			 */
			@NonNull
			public SuspendDialogInfo build() {
				return new SuspendDialogInfo(this);
			}
		}
	}
}