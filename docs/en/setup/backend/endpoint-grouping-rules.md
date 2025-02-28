# Group Parameterized Endpoints
In most cases, endpoints are detected automatically through language agents, service mesh observability solutions,
or meter system configurations.

There are some special cases, especially when REST-style URI is used, where the application codes include the parameter in the endpoint name,
such as putting order ID in the URI. Examples are `/prod/ORDER123` and `/prod/ORDER456`. But logically, most would expect to
have an endpoint name like `prod/{order-id}`. This is a specially designed feature in parameterized endpoint grouping.

If the incoming endpoint name accords with the rules, SkyWalking will group the endpoint by rules.

There are two approaches in which SkyWalking supports endpoint grouping:
1. Endpoint name grouping by OpenAPI definitions.
2. Endpoint name grouping by custom configurations.

Both grouping approaches can work together in sequence.

## Endpoint name grouping by OpenAPI definitions
The OpenAPI definitions are documents based on the [OpenAPI Specification (OAS)](https://www.openapis.org/), which is used to define a standard, language-agnostic interface for HTTP APIs.

SkyWalking now supports `OAS v2.0+`. It could parse the documents `(yaml)` and build grouping rules from them automatically.


### How to use
1. Add `Specification Extensions` for SkyWalking config in the OpenAPI definition documents; otherwise, all configs are default:<br />
   `${METHOD}` is a reserved placeholder which represents the HTTP method, e.g. `POST/GET...` <br />.
   `${PATH}` is a reserved placeholder which represents the path, e.g. `/products/{id}`.

   | Extension Name | Required | Description | Default Value |
   |-----|-----|-----|-----|
   | x-sw-service-name | false | The service name to which these endpoints belong. | The directory name to which the OpenAPI definition documents belong. |
   | x-sw-endpoint-name-match-rule | false | The rule used to match the endpoint. | `${METHOD}:${PATH}` |
   | x-sw-endpoint-name-format | false | The endpoint name after grouping. | `${METHOD}:${PATH}` |

   These extensions are under `OpenAPI Object`. For example, the document below has a full custom config:

``` yaml
openapi: 3.0.0
x-sw-service-name: serviceB
x-sw-endpoint-name-match-rule: "${METHOD}:${PATH}"
x-sw-endpoint-name-format: "${METHOD}:${PATH}"

info:
  description: OpenAPI definition for SkyWalking test.
  version: v2
  title: Product API
  ...
```

   We highly recommend using the default config. The custom config (`x-sw-endpoint-name-match-rule/x-sw-endpoint-name-format`) is considered part of the match rules (regex pattern).
   We have provided some use cases in `org.apache.skywalking.oap.server.core.config.group.openapi.EndpointGroupingRuleReader4OpenapiTest`. You may validate your custom config as well.

2. All OpenAPI definition documents are located in the `openapi-definitions` directory, with directories having at most two levels. We recommend using the service name as the subDirectory name, as you will then not be required to set `x-sw-service-name`. For example:
  ```
├── openapi-definitions
│   ├── serviceA
│   │   ├── customerAPI-v1.yaml
│   │   └── productAPI-v1.yaml
│   └── serviceB
│       └── productAPI-v2.yaml
```
3. The feature is enabled by default. You can disable it by setting the `Core Module` configuration `${SW_CORE_ENABLE_ENDPOINT_NAME_GROUPING_BY_OPAENAPI:false}`.

### Rules match priority 
We recommend designing the API path as clearly as possible. If the API path is fuzzy and an endpoint name matches multiple paths, SkyWalking would select a path according to the match priority set out below:
1. The exact path is matched. 
   E.g. `/products or /products/inventory`
2. The path with fewer variables.
   E.g. In the case of `/products/{var1}/{var2} and /products/{var1}/abc`, endpoint name `/products/123/abc` will match the second one.
3. If the paths have the same number of variables, the longest path is matched, and the vars are considered to be `1`.
   E.g. In the case of `/products/abc/{var1} and products/{var12345}/ef`, endpoint name `/products/abc/ef` will match the first one, because `length("abc") = 3` is larger than `length("ef") = 2`.
### Examples
If we have an OpenAPI definition doc `productAPI-v2.yaml` in directory `serviceB`, it will look like this:
```yaml

openapi: 3.0.0

info:
  description: OpenAPI definition for SkyWalking test.
  version: v2
  title: Product API

tags:
  - name: product
    description: product
  - name: relatedProducts
    description: Related Products

paths:
  /products:
    get:
      tags:
        - product
      summary: Get all products list
      description: Get all products list.
      operationId: getProducts
      responses:
        "200":
          description: Success
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: "#/components/schemas/Product"
  /products/{region}/{country}:
    get:
      tags:
        - product
      summary: Get products regional
      description: Get products regional with the given id.
      operationId: getProductRegional
      parameters:
        - name: region
          in: path
          description: Products region
          required: true
          schema:
            type: string
        - name: country
          in: path
          description: Products country
          required: true
          schema:
            type: string
      responses:
        "200":
          description: successful operation
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/Product"
        "400":
          description: Invalid parameters supplied
  /products/{id}:
    get:
      tags:
        - product
      summary: Get product details
      description: Get product details with the given id.
      operationId: getProduct
      parameters:
        - name: id
          in: path
          description: Product id
          required: true
          schema:
            type: integer
            format: int64
      responses:
        "200":
          description: successful operation
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ProductDetails"
        "400":
          description: Invalid product id
    post:
      tags:
        - product
      summary: Update product details
      description: Update product details with the given id.
      operationId: updateProduct
      parameters:
        - name: id
          in: path
          description: Product id
          required: true
          schema:
            type: integer
            format: int64
        - name: name
          in: query
          description: Product name
          required: true
          schema:
            type: string
      responses:
        "200":
          description: successful operation
    delete:
      tags:
        - product
      summary: Delete product details
      description: Delete product details with the given id.
      operationId: deleteProduct
      parameters:
        - name: id
          in: path
          description: Product id
          required: true
          schema:
            type: integer
            format: int64
      responses:
        "200":
          description: successful operation
  /products/{id}/relatedProducts:
    get:
      tags:
        - relatedProducts
      summary: Get related products
      description: Get related products with the given product id.
      operationId: getRelatedProducts
      parameters:
        - name: id
          in: path
          description: Product id
          required: true
          schema:
            type: integer
            format: int64
      responses:
        "200":
          description: successful operation
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/RelatedProducts"
        "400":
          description: Invalid product id

components:
  schemas:
    Product:
      type: object
      description: Product id and name
      properties:
        id:
          type: integer
          format: int64
          description: Product id
        name:
          type: string
          description: Product name
      required:
        - id
        - name
    ProductDetails:
      type: object
      description: Product details
      properties:
        id:
          type: integer
          format: int64
          description: Product id
        name:
          type: string
          description: Product name
        description:
          type: string
          description: Product description
      required:
        - id
        - name
    RelatedProducts:
      type: object
      description: Related Products
      properties:
        id:
          type: integer
          format: int32
          description: Product id
        relatedProducts:
          type: array
          description: List of related products
          items:
            $ref: "#/components/schemas/Product"


```

Here are some use cases:

   | Incoming Endpiont | Incoming Service | x-sw-service-name | x-sw-endpoint-name-match-rule | x-sw-endpoint-name-format | Matched | Grouping Result |
   |-----|-----|-----|-----|-----|-----|-----|
   | `GET:/products` | serviceB | default | default | default | true | `GET:/products` |
   | `GET:/products/123` | serviceB | default | default | default |  true | `GET:/products{id}` |
   | `GET:/products/asia/cn` | serviceB | default | default | default | true | `GET:/products/{region}/{country}` |
   | `GET:/products/123/abc/efg` | serviceB | default | default | default |  false | `GET:/products/123/abc/efg` | 
   | `<GET>:/products/123` | serviceB | default | default | default | false | `<GET>:/products/123`|
   | `GET:/products/123` | serviceC | default | default | default | false | `GET:/products/123` |
   | `GET:/products/123` | serviceC | serviceC | default | default | true | `GET:/products/123` |
   | `<GET>:/products/123` | serviceB | default | `<${METHOD}>:${PATH}` | `<${METHOD}>:${PATH}` | true | `<GET>:/products/{id}` |
   | `GET:/products/123` | serviceB | default | default | `${PATH}:<${METHOD}>` | true | `/products/{id}:<GET>` |
   | `/products/123:<GET>` | serviceB | default | `${PATH}:<${METHOD}>` | default | true | `GET:/products/{id}` |

### Initialize and update the OpenAPI definitions dynamically
Use [Dynamic Configuration](dynamic-config.md) to initialize and update OpenAPI definitions, the endpoint grouping rules from OpenAPI
will re-create by the new config.


## Endpoint name grouping by custom configuration
Currently, a user could set up grouping rules through the static YAML file named `endpoint-name-grouping.yml`,
or use [Dynamic Configuration](dynamic-config.md) to initialize and update endpoint grouping rules.

### Configuration Format
Both the static local file and dynamic configuration value share the same YAML format.

```yaml
grouping:
  # Endpoint of the service would follow the following rules
  - service-name: serviceA
    rules:
      # Logic name when the regex expression matched.
      - endpoint-name: /prod/{id}
        regex: \/prod\/.+
```
