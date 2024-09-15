import sys
import os

try:
    import service_inventory_client
except ModuleNotFoundError:
    # allow running component locally without setting PYTHONPATH
    sys.path.append(os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '../src')))
    import service_inventory_client

from request_file_mocker import RequestFileMocker

from service_inventory_client import ServiceInventoryAPI
import json


BASE_URL="https://canvas-info.ihc-dt.cluster-3.de/tmf-api/serviceInventoryManagement/v5"
RM_TESTDATA_FOLDER = "testdata/requests_mock"


def clean_all(svc_inv:ServiceInventoryAPI):
    svcs = svc_inv.list_services(state=None)
    ids = [svc["id"] for svc in svcs]
    for id in ids:
        print(f"deleting service {id}")
        svc_inv.delete_service(id)
    

def test_service_inventory_api():
    svc_inv = ServiceInventoryAPI(BASE_URL)
    clean_all(svc_inv)

    rfmock = RequestFileMocker(RM_TESTDATA_FOLDER, BASE_URL, recording=False)

    rfmock.mock_get('service', 'list-all-initial-empty', 200)
    svcs = svc_inv.list_services(state=None)
    print(f"\nLIST ALL SERVICES:\n{json.dumps(svcs, indent=2)}")
    
    rfmock.mock_post('service', "create-active-acme-downstream", 201)
    svc1 = svc_inv.create_service(
        componentName="acme-productinventory", 
        dependencyName="downstreamproductcatalog", 
        url="http://components.ihc-dt.cluster-3.de/alice-productcatalogmanagement/tmf-api/productCatalogManagement/v4",
        specification="https://raw.githubusercontent.com/tmforum-apis/TMF620_ProductCatalog/master/TMF620-ProductCatalog-v4.0.0.swagger.json",
        state="active")
    print(f"\nCREATED active acme downstream SERVICE 1:\n{json.dumps(svc1,indent=2)}\n")

    rfmock.mock_post('service', "create-inactive-bcme-downstream", 201)
    svc2 = svc_inv.create_service(
        componentName="bcme-productinventory", 
        dependencyName="downstreamproductcatalog", 
        url="http://components.ihc-dt.cluster-3.de/alice-productcatalogmanagement/tmf-api/productCatalogManagement/v4",
        specification="https://raw.githubusercontent.com/tmforum-apis/TMF620_ProductCatalog/master/TMF620-ProductCatalog-v4.0.0.swagger.json",
        state="inactive")
    print(f"\nCREATED inactive bcme downstream SERVICE 2:\n{json.dumps(svc2,indent=2)}\n")

    rfmock.mock_post('service', "create-active-bcme-upstream", 201)
    svc3 = svc_inv.create_service(
        componentName="bcme-productinventory",
        dependencyName="upstreamproductcatalog",
        url="http://components.ihc-dt.cluster-3.de/alice-productcatalogmanagement/tmf-api/productCatalogManagement/v4",
        specification="https://raw.githubusercontent.com/tmforum-apis/TMF620_ProductCatalog/master/TMF620-ProductCatalog-v4.0.0.swagger.json",
        state="active")
    print(f"\nCREATED active bcme upstream SERVICE 3:\n{json.dumps(svc3,indent=2)}\n")

    rfmock.mock_get('service', 'list-all', 200)
    svcs = svc_inv.list_services(state=None)
    print(f"\nALL SERVICES 123:\n{json.dumps(svcs, indent=2)}")

    rfmock.mock_get('service', 'list-active', 200)
    svcs = svc_inv.list_services()
    print(f"\nALL active SERVICES 13:\n{json.dumps(svcs, indent=2)}")

    rfmock.mock_get('service', 'list-active-bcme', 200)
    svcs = svc_inv.list_services(component_name="bcme-productinventory")
    print(f"\nLIST active bcme SERVICES 3:\n{json.dumps(svcs, indent=2)}")

    rfmock.mock_get('service', 'list-active-downstream', 200)
    svcs = svc_inv.list_services(dependency_name="downstreamproductcatalog")
    print(f"\nLIST active downstream SERVICES 1:\n{json.dumps(svcs, indent=2)}")

    rfmock.mock_get('service', 'list-bcme', 200)
    svcs = svc_inv.list_services(component_name="bcme-productinventory", state=None)
    print(f"\nLIST bcme SERVICES 23:\n{json.dumps(svcs, indent=2)}")

    rfmock.mock_get('service', 'list-active-bcme-downstream', 200)
    svcs = svc_inv.list_services(component_name="bcme-productinventory", dependency_name="downstreamproductcatalog")
    print(f"\nLIST active bcme downstream SERVICES -:\n{json.dumps(svcs, indent=2)}")

    rfmock.mock_get(f'service/{svc2["id"]}', 'id-svc2', 200)
    svc2b = svc_inv.get_service(svc2["id"])
    print(f"\nGET service-2 BY ID:\n{json.dumps(svc2b, indent=2)}")

    rfmock.mock_patch(f'service/{svc2["id"]}', 'update-svc2-active', 200)
    svc2c = svc_inv.update_service(
        id=svc2["id"],
        componentName=svc2["componentName"], 
        dependencyName=svc2["dependencyName"], 
        url=svc2["url"],
        specification=svc2["OASSpecification"],
        state="active")
    print(f"\nUPDATED service-2 state to active:\n{json.dumps(svc2c,indent=2)}\n")
    
    rfmock.mock_get('service', 'update-list-active-downstream', 200)
    svcs = svc_inv.list_services(dependency_name="downstreamproductcatalog")
    print(f"\nLIST active downstream SERVICES 12:\n{json.dumps(svcs, indent=2)}")

    rfmock.mock_get('service', 'update-list-active-bcme-downstream', 200)
    svcs = svc_inv.list_services(component_name="bcme-productinventory", dependency_name="downstreamproductcatalog")
    print(f"\nLIST active bcme downstream SERVICES 2:\n{json.dumps(svcs, indent=2)}")

    rfmock.mock_delete(f'service/{svc1["id"]}', 'svc1', 204)
    svc_inv.delete_service(svc1["id"])
    print(f"\ndeleted service-1")

    rfmock.mock_delete(f'service/{svc2["id"]}', 'svc2', 204)
    svc_inv.delete_service(svc2["id"])
    print(f"\ndeleted service-2")

    rfmock.mock_get('service', 'del12-list-active', 200)
    svcs = svc_inv.list_services()
    print(f"\n[DEL12] ALL active SERVICES 3:\n{json.dumps(svcs, indent=2)}")

    rfmock.mock_delete(f'service/{svc3["id"]}', 'svc3', 204)
    svc_inv.delete_service(svc3["id"])
    print(f"\ndeleted service-3")

    rfmock.mock_get('service', 'del123-list-allstates', 200)
    svcs = svc_inv.list_services(state=None)
    print(f"\n[DEL123] ALL active SERVICES 3:\n{json.dumps(svcs, indent=2)}")

    try:
        rfmock.mock_get(f'service/{svc3["id"]}', 'id-unknown', 500)
        _ = svc_inv.get_service(svc3["id"])
        raise Exception(f'GET AFTER DELETE WAS SUCCESSFUL FOR {svc3["id"]}')
    except ValueError as e:
        print(f"GET SERVICE expected error: {e}")



if __name__ == "__main__":
    test_service_inventory_api()