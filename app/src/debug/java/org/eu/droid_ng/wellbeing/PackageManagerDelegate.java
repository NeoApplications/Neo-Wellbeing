package org.eu.droid_ng.wellbeing;

import android.content.pm.PackageManager;
import android.os.PersistableBundle;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/* both PackageManager stub for building in Android Studio for UI stuff
* and reflective delegate for magisk module, only used in debug builds.
*
* Note: The class must not fail or crash if a reference is missing.
* */
public class PackageManagerDelegate {
	private static boolean success;
	private static Method realSuspendDialogInfoBuilderBuild;
	private static Method setPackagesSuspended;
	private static Constructor<?> realSuspendDialogInfoCst;
	private static Constructor<?> realSuspendDialogInfoBuilderCst;
	private static Method getIconResId;
	private static Method setIconResId;
	private static Method getTitleResId;
	private static Method setTitleResId;
	private static Method getDialogMessageResId;
	private static Method setDialogMessageResId;
	private static Method getDialogMessage;
	private static Method setDialogMessage;
	private static Method getNeutralButtonTextResId;
	private static Method setNeutralButtonTextResId;
	private static Method getNeutralButtonAction;
	private static Method setNeutralButtonAction;

	static {
		HiddenApiBypass.addHiddenApiExemptions(""); // Help in some cases.
		try {
			Class<?> realSuspendDialogInfo = Class.forName("android.content.pm.SuspendDialogInfo");
			Class<?> realSuspendDialogInfoBuilder = Class.forName("android.content.pm.SuspendDialogInfo$Builder");
			setPackagesSuspended = PackageManager.class.getDeclaredMethod("setPackagesSuspended", String[].class,
					boolean.class, PersistableBundle.class, PersistableBundle.class, realSuspendDialogInfo);
			try {
				realSuspendDialogInfoBuilderBuild =
						realSuspendDialogInfoBuilder.getMethod("build");
				if (realSuspendDialogInfoBuilderBuild.getReturnType() != realSuspendDialogInfo) {
					realSuspendDialogInfoBuilderBuild = null;
				}
			} catch (ReflectiveOperationException e) {
				realSuspendDialogInfoBuilderBuild = null;
			}
			if (realSuspendDialogInfoBuilderBuild == null) {
				realSuspendDialogInfoCst =
						realSuspendDialogInfo.getConstructor(realSuspendDialogInfoBuilder);
			}
			realSuspendDialogInfoBuilderCst =
					realSuspendDialogInfoBuilder.getConstructor();
			getIconResId = realSuspendDialogInfo.getMethod("getIconResId");
			setIconResId = realSuspendDialogInfoBuilder.getMethod("setIcon", int.class);
			getTitleResId = realSuspendDialogInfo.getMethod("getTitleResId");
			setTitleResId = realSuspendDialogInfoBuilder.getMethod("setTitle", int.class);
			getDialogMessageResId = realSuspendDialogInfo.getMethod("getDialogMessageResId");
			setDialogMessageResId = realSuspendDialogInfoBuilder.getMethod("setMessage", int.class);
			getDialogMessage = realSuspendDialogInfo.getMethod("getDialogMessage");
			setDialogMessage = realSuspendDialogInfoBuilder.getMethod("setMessage", String.class);
			getNeutralButtonTextResId = realSuspendDialogInfo.getMethod("getNeutralButtonTextResId");
			setNeutralButtonTextResId = realSuspendDialogInfoBuilder.getMethod("setNeutralButtonText", int.class);
			try {
				getNeutralButtonAction = realSuspendDialogInfo.getMethod("getNeutralButtonAction");
				setNeutralButtonAction = realSuspendDialogInfoBuilder.getMethod("setNeutralButtonAction", int.class);
			} catch (ReflectiveOperationException e) {
				getNeutralButtonAction = null;
				setNeutralButtonAction = null;
			}
			success = true;
		} catch (ReflectiveOperationException e) {
			Log.e("PackageManagerDelegate", // Log why it's crashing
					"This would not occur if the app was built-in into the ROM:", e);
			success = false;
		}
	}

	private final PackageManager pm;

	public PackageManagerDelegate(PackageManager pm) {
		this.pm = pm;
	}

	public String[] setPackagesSuspended(@Nullable String[] packageNames, boolean suspend, @Nullable PersistableBundle appExtras, @Nullable PersistableBundle launcherExtras, @Nullable SuspendDialogInfo dialogInfo) {
		if (success && (dialogInfo == null || dialogInfo.real != null)) {
			try {
				setPackagesSuspended.invoke(this.pm, packageNames, suspend, appExtras,
						launcherExtras, dialogInfo == null ? null : dialogInfo.real);
			} catch (ReflectiveOperationException ignored) {}
		}

		/* stub */
		return new String[]{};
	}

	public static class SuspendDialogInfo {
		Object real; // Instance used by reflection
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
			if (success && real != null) {
				try {
					return (Integer) getIconResId.invoke(real);
				} catch (ReflectiveOperationException ignored) {}
			}
			return 0;
		}

		/**
		 * @return the resource id of the title to be used with the dialog
		 * @hide
		 */
		@StringRes
		public int getTitleResId() {
			if (success && real != null) {
				try {
					return (Integer) getTitleResId.invoke(real);
				} catch (ReflectiveOperationException ignored) {}
			}
			return 0;
		}

		/**
		 * @return the resource id of the text to be shown in the dialog's body
		 * @hide
		 */
		@StringRes
		public int getDialogMessageResId() {
			if (success && real != null) {
				try {
					return (Integer) getDialogMessageResId.invoke(real);
				} catch (ReflectiveOperationException ignored) {}
			}
			return 0;
		}

		/**
		 * @return the text to be shown in the dialog's body. Returns {@code null} if {@link
		 * #getDialogMessageResId()} returns a valid resource id
		 * @hide
		 */
		@Nullable
		public String getDialogMessage() {
			if (success && real != null) {
				try {
					return (String) getDialogMessage.invoke(real);
				} catch (ReflectiveOperationException ignored) {}
			}
			return "";
		}

		/**
		 * @return the text to be shown
		 * @hide
		 */
		@StringRes
		public int getNeutralButtonTextResId() {
			if (success && real != null) {
				try {
					return (Integer) getNeutralButtonTextResId.invoke(real);
				} catch (ReflectiveOperationException ignored) {}
			}
			return 0;
		}

		/**
		 * @return The {@link ButtonAction} that happens on tapping this button
		 */
		@ButtonAction
		public int getNeutralButtonAction() {
			if (success && real != null && getNeutralButtonAction != null) {
				try {
					return (Integer) getNeutralButtonAction.invoke(real);
				} catch (ReflectiveOperationException ignored) {}
			}
			return 0;
		}

		@Override
		public int hashCode() {
			return 0;
		}

		SuspendDialogInfo(Builder b) {
			if (success && b != null && b.realB != null) {
				try {
					if (realSuspendDialogInfoBuilderBuild != null) {
						this.real = realSuspendDialogInfoBuilderBuild.invoke(b.realB);
					} else {
						this.real = realSuspendDialogInfoCst.newInstance(b.realB);
					}
				} catch (ReflectiveOperationException ignored) {}
			}
		}

		/**
		 * Builder to build a {@link SuspendDialogInfo} object.
		 */
		public static final class Builder {
			Object realB; // Instance used by reflection

			public Builder() {
				if (success) {
					try {
						this.realB = realSuspendDialogInfoBuilderCst.newInstance();
					} catch (ReflectiveOperationException ignored) {}
				}
			}
			/**
			 * Set the resource id of the icon to be used. If not provided, no icon will be shown.
			 *
			 * @param resId The resource id of the icon.
			 * @return this builder object.
			 */
			@NonNull
			public Builder setIcon(@DrawableRes int resId) {
				if (success && realB != null) {
					try {
						setIconResId.invoke(realB, resId);
					} catch (ReflectiveOperationException ignored) {}
				}
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
				if (success && realB != null) {
					try {
						setTitleResId.invoke(realB, resId);
					} catch (ReflectiveOperationException ignored) {}
				}
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
				if (success && realB != null) {
					try {
						setDialogMessage.invoke(realB, message);
					} catch (ReflectiveOperationException ignored) {}
				}
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
				if (success && realB != null) {
					try {
						setDialogMessageResId.invoke(realB, resId);
					} catch (ReflectiveOperationException ignored) {}
				}
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
				if (success && realB != null) {
					try {
						setNeutralButtonTextResId.invoke(realB, resId);
					} catch (ReflectiveOperationException ignored) {}
				}
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
				if (success && realB != null && setNeutralButtonAction != null) {
					try {
						setNeutralButtonAction.invoke(realB, buttonAction);
					} catch (ReflectiveOperationException ignored) {}
				}
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

	public static boolean canSuspend() {
		return success;
	}

	public static boolean canSetNeutralButtonAction() {
		return success && setNeutralButtonAction != null;
	}
}