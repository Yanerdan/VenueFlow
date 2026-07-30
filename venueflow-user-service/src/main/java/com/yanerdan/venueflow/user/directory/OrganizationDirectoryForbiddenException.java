package com.yanerdan.venueflow.user.directory;

public class OrganizationDirectoryForbiddenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public OrganizationDirectoryForbiddenException() {
    super("Current role cannot administer the organization directory");
  }
}
