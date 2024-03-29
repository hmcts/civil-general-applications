package uk.gov.hmcts.reform.civil.validation;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.net.IDN.toASCII;
import static java.net.IDN.toUnicode;
import static java.util.Collections.emptyList;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.split;

@Slf4j
@Service
public class ValidateEmailService {

    private static final int EMAIL_MAX_LENGTH = 320;
    private static final int HOST_MAX_LENGTH = 253;
    private static final int HOST_PART_MAX_LENGTH = 63;
    private static final int USERNAME_MAX_LENGTH = 64;

    private static final String ERROR_MESSAGE = "Enter an email address in the correct format,"
        + " for example john.smith@example.com";
    private static final String LOCAL_CHARS = "a-zA-Z0-9.!#$%&'*+/=?^_`{|}~\\-";
    private static final Pattern HOSTNAME_PATTERN = compile(format(
        "^(xn-|[a-z0-9]{1,%1$d})(-[a-z0-9]{1,%1$d}){0,%1$d}$", HOST_PART_MAX_LENGTH), CASE_INSENSITIVE);
    private static final Pattern TLD_PATTERN = compile(format(
        "^([a-z]{2,%1$d}|xn--([a-z0-9]{1,%1$d}-){0,%1$d}[a-z0-9]{1,%1$d})$", HOST_PART_MAX_LENGTH), CASE_INSENSITIVE);
    private static final Pattern EMAIL_PATTERN = compile(format(
        "^[%s]{1,%2$d}@([^.@][^@\\s]{2,%2$d})$", LOCAL_CHARS, EMAIL_MAX_LENGTH));

    public List<String> validate(String email) {
        return isValid(email) ? emptyList() : List.of(ERROR_MESSAGE);
    }

    /*
    Mimic gov notify validation
    see https://github.com/alphagov/notifications-utils/blob/master/notifications_utils/recipients.py#L494-L534
     */
    private boolean isValid(String email) {
        if (!validateEmail(email)) {
            return false;
        }

        final String emailAddress = StringUtils.trim(email);

        final String username = emailAddress.split("@")[0];

        if (!validateUsername(username)) {
            return false;
        }

        final Matcher emailMatcher = EMAIL_PATTERN.matcher(emailAddress);

        if (!emailMatcher.matches()) {
            log.warn("Email does not match pattern");
            return false;
        }

        String hostname = emailMatcher.group(1);

        return validateHost(hostname);
    }

    private boolean validateEmail(String email) {
        if (isEmpty(email)) {
            log.warn("Email is null or empty");
            return false;
        }

        final String emailAddress = StringUtils.trim(email);

        if (emailAddress.startsWith(".")) {
            log.warn("Email begins with .");
            return false;
        }
        if (emailAddress.length() > EMAIL_MAX_LENGTH) {
            log.warn("Email is longer than {} characters", EMAIL_MAX_LENGTH);
            return false;
        }

        if (emailAddress.contains("..")) {
            log.warn("Email contains ..");
            return false;
        }
        return true;
    }

    private boolean validateUsername(String username) {
        if (username.endsWith(".")) {
            log.warn("Username ends with .");
            return false;
        }
        if (username.length() > USERNAME_MAX_LENGTH) {
            log.warn("Email username is longer than {} characters", USERNAME_MAX_LENGTH);
            return false;
        }
        return true;
    }

    private boolean validateHost(String hostname) {
        try {
            hostname = toASCII(toUnicode(hostname));
        } catch (Exception e) {
            log.warn("Email hostname can not be converted to ascii");
            return false;
        }

        final String[] hostParts = split(hostname, ".");

        if (hostname.length() > HOST_MAX_LENGTH) {
            log.warn("Email hostname is longer than {} characters", HOST_MAX_LENGTH);
            return false;
        }

        if (hostParts.length < 2) {
            log.warn("Email hostname parts is {}", hostParts.length);
            return false;
        }

        for (String hostPart : hostParts) {
            if (hostPart.length() > HOST_PART_MAX_LENGTH) {
                log.warn("Email hostname part is longer than {}", HOST_PART_MAX_LENGTH);
                return false;
            }

            if (!HOSTNAME_PATTERN.matcher(hostPart).matches()) {
                log.warn("Email hostname part does not match pattern");
                return false;
            }
        }

        if (!TLD_PATTERN.matcher(hostParts[hostParts.length - 1]).matches()) {
            log.warn("Email top level domain does not match pattern");
            return false;
        }
        return true;
    }
}
