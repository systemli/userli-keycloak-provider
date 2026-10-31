<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('backupCode'); section>
    <#if section = "header">
        ${msg("userli-backup-code-header")}
    <#elseif section = "form">
        <form id="kc-userli-backup-code-form" class="${properties.kcFormClass!}" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="backupCode" class="${properties.kcLabelClass!}">${msg("userli-backup-code-prompt")}</label>
                </div>

                <div class="${properties.kcInputWrapperClass!}">
                    <input id="backupCode"
                           name="backupCode"
                           aria-invalid="<#if messagesPerField.existsError('backupCode')>true</#if>"
                           autocomplete="off"
                           type="text"
                           class="${properties.kcInputClass!}"
                           inputmode="numeric"
                           autofocus
                           dir="ltr"/>

                    <#if messagesPerField.existsError('backupCode')>
                        <span id="input-error" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('backupCode'))?no_esc}
                        </span>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                           name="login" id="kc-login" type="submit" value="${msg("doLogIn")}"/>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
