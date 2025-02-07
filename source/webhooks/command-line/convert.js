// FILE: app.js
const fs = require('fs');
const yaml = require('js-yaml');
const http = require('http');

// Function to load and parse YAML file
function loadYamlFile(filePath) {
    try {
        // console.log(`Loading file: ${filePath}`);
        let fileContents = fs.readFileSync(filePath, 'utf8');
        fileContents = fileContents.replace(/\r\n/g, '\n'); // Normalize line endings

        // show a warning if the file contains {{- if
        if (fileContents.includes('{{- if')) {
            console.warn('Warning: The file contains {{- if xxx }} clauses which may cause issues with the conversion');
            process.exit(1);
        } 

        fileContents = fileContents.replace(/\{\{\s+/g, '{{').replace(/\s+\}\}/g, '}}');
        fileContents = fileContents.replace(/\{\{([^}]+)\}\}/g, (match, p1) => `{{${p1.replace(/\./g, 'PPP')}}}`);
        fileContents = fileContents.replace(/\{\{/g, 'XX').replace(/\}\}/g, 'YY');

        // console.log(fileContents);
        return yaml.load(fileContents);
    } catch (e) {
        console.error(`Error loading YAML file: ${e.message}`);
        process.exit(1);
    }
}


// Function to create ConversionReview object
function createConversionReview(component, inDesiredVersion) {
    return {
        apiVersion: 'apiextensions.k8s.io/v1',
        kind: 'ConversionReview',
        request: {
            uid: '0000',
            desiredAPIVersion: 'oda.tmforum.org/' + inDesiredVersion,
            apiVersion: component.apiVersion,
            objects: [component],
        },
    };
}

// Main function
function main() {
    const args = process.argv.slice(2);
    if (args.length < 2) {
        console.error('Usage: npm start <path-to-yaml-file> <new-version> [output-file]');
        process.exit(1);
    }

    const yamlFilePath = args[0];
    const newVersion = args[1];
    const outputFilePath = args[2]; // Optional third parameter
    console.log(`Converting ${yamlFilePath} to version ${newVersion}`);
    console.log(`Output file: ${outputFilePath}`);

    const component = loadYamlFile(yamlFilePath);
    const conversionReview = createConversionReview(component, newVersion);

    // console.log(JSON.stringify(conversionReview, null, 2));

    const postData = JSON.stringify(conversionReview);

    const options = {
        hostname: 'localhost',
        port: 8002,
        path: '/',
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Content-Length': Buffer.byteLength(postData),
        },
    };
    
    const req = http.request(options, (res) => {
        // console.log(`statusCode: ${res.statusCode}`);
    
        // store the result in a variable
        let data = '';
        res.on('data', (chunk) => {
            data += chunk;
        });

        res.on('end', () => {
            const result = JSON.parse(data);
            const convertedObject = result.response.convertedObjects[0];
            let yamlOutput = yaml.dump(convertedObject);

            // Replace tokens back to original
            yamlOutput = yamlOutput.replace(/XX/g, '{{').replace(/PPP/g, '.').replace(/YY/g, '}}');

            if (outputFilePath) {
                fs.writeFileSync(outputFilePath, yamlOutput, 'utf8');
                console.log(`Converted object saved to ${outputFilePath}`);
            } else {
                // print as YAML
                console.log(Buffer.from(yamlOutput).toString());
            }
        });
        
    });
    
    req.on('error', (e) => {
        console.error(`Problem with request: ${e.message}`);
    });
    
    // Write data to request body
    req.write(postData);
    req.end();
}

main();