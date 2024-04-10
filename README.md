# XtremeXP Visualization API

Welcome to the XtremeXP Visualization API! This API is designed to provide visual analytics and explainability features for your machine learning models. Leveraging RESTful endpoints and utilizing the GRPC Explainability API developed by ARC, this tool aims to enhance the interpretability of your models while making it easy to integrate into your existing infrastructure.

## Features

- **Visual Analytics**: Gain insights into your data and machine learning models through interactive visualizations.
- **Visual Explainability**: Understand the inner workings of your models through visual explanations.
- **REST API**: Communicate with the API easily through RESTful endpoints.
- **Docker Compose Installation**: Simple deployment using Docker Compose for easy setup and scalability.

## Installation

1. Clone this repository:

    ```
   https://colab-repo.intracom-telecom.com/colab-projects/extremexp/user-interaction/visualization/vis-api.git
    ```

2. Navigate into the project directory:

    ```
    cd vis-api
    ```

3. Install Docker and Docker Compose if you haven't already.

4. Run Docker Compose to build and start the containers:

    ```
    docker-compose up --build
    ```

5. Once the containers are up and running, you can access the API at `http://localhost:8080`.

## Usage

### API Endpoints

### Visual Analytics Request

- **POST /visualization/data/{dataset_id}**

```json
{
    "visualizationType": "line",
    "columns": [],
    "aggFunction": "AVG",
    "groupBy": [],
    "filters": [],
    "constraints": {},
    "taskId": "1",
    "limit": 1001
}
```
**Description:**
This request is used to retrieve visual analytics data. It specifies parameters such as visualization type (e.g., "line"), aggregation function (e.g., "AVG"), group by columns, filters, constraints, task ID, and limit.


### Explainability Pipeline Request

- **POST /explainability/{dataset_id}**

```json
{
    "model_id": "classifier",
    "explainabilityType": "pipeline",
    "explainabilityMethod":"pdp",
    "visualizationType": "line",
    "constraints": {},
    "additionalParams": {
        "feature" : "feature1"
    }
}
```

**Description:**
This request initiates an explainability pipeline to generate explanations for a classifier model. It specifies the model ID, explainability type (e.g., "pipeline"), explainability method (e.g., "pdp"), visualization type (e.g., "line"), constraints, and additional parameters (e.g., "feature").

### Example Usage

```python
import requests

# Example request for analytics
analytics_response = requests.get('http://localhost:8080/api/visualization/data/i2cat_desktop_features')
print(analytics_response.json())

# Example request for explanations
explain_response = requests.get('http://localhost:8080/api/explainability/i2cat_desktop_features')
print(explain_response.json())
```

## Dependencies

- Docker
- Docker Compose

## License
Not available

Feel free to reach out with any questions, issues, or suggestions!