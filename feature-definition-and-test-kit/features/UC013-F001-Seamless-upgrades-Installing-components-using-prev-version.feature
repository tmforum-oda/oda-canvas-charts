# The 'business stakeholders' for the canvas Behaviour Driven Design are the Engineering teams
# from the Vendor of a component, from a Systems Integrator who may be integrating and deploying 
# a component, or a Service Provider's Operations team who may be supporting a component.

@UC013         # tagged as use case 13
@UC013-F001    # tagged as use feature 1 within use case 13
Feature: UC013-F001 Seamless upgrade: Installing component using previous version

    Scenario: Installing a component using a previous (N-1) version
        Given an example package 'productcatalog-v1beta4' with a 'productcatalogmanagement' component with '1' ExposedAPI in its 'coreFunction' segment
        When I install the 'productcatalog-v1beta4' package
        And the 'productcatalogmanagement' component has a deployment status of 'Complete'
        Then I can query the 'v1' spec version of the 'productcatalogmanagement' component

    Scenario: Installing a component using a previous (N-2) version
        Given an example package 'productcatalog-v1beta3' with a 'productcatalogmanagement' component with '1' ExposedAPI in its 'coreFunction' segment
        When I install the 'productcatalog-v1beta3' package
        And the 'productcatalogmanagement' component has a deployment status of 'Complete'
        Then I can query the 'v1' spec version of the 'productcatalogmanagement' component

    Scenario: Installing a component with Dependent APIs using a previous (N-1) version
        # Install the federated productcatalog component that has a dependency on a downstream  productcatalog as release f-cat
        When I install the 'productcatalog-dependendent-API-v1beta4' package as release 'f-cat'
        Then I should see the 'downstreamproductcatalog' DependentAPI resource on the 'productcatalogmanagement' component with a ready status

    Scenario: Installing a component with Dependent APIs using a previous (N-2) version
        # Install the federated productcatalog component that has a dependency on a downstream  productcatalog as release f-cat
        When I install the 'productcatalog-dependendent-API-v1beta3' package as release 'f-cat'
        Then I should see the 'downstreamproductcatalog' DependentAPI resource on the 'productcatalogmanagement' component with a ready status
