package com.yanerdan.venueflow.common.core.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "com.yanerdan.venueflow.common",
    importOptions = ImportOption.DoNotIncludeTests.class)
class CommonModuleArchitectureTest {

  @ArchTest
  static final ArchRule COMMON_MUST_NOT_DEPEND_ON_BUSINESS_DOMAINS =
      noClasses()
          .that()
          .resideInAPackage("..common..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..auth..",
              "..user..",
              "..resource..",
              "..booking..",
              "..notification..",
              "..search..");

  private CommonModuleArchitectureTest() {}
}
