# Secrets and Credentials Policy

- Never commit credentials, private keys, access tokens, passwords, or
  production connection strings.
- Store release credentials only in GitHub Actions secrets or protected
  environments.
- Grant secrets only to jobs that require them.
- Pull request workflows from untrusted code must not receive release secrets or
  write tokens.
- Prefer short-lived OIDC credentials and artifact attestations over persistent
  signing secrets when the target service supports them.
- Rotate a credential immediately after suspected exposure and revoke the old
  value before investigating further.
- Review long-lived release credentials at least every six months.
- Use separate credentials for Maven Central, GitHub Packages, and PGP signing.
- Do not print secrets, complete GitHub contexts, or secret-derived values in
  logs.

GitHub secret scanning and push protection are required repository controls.
