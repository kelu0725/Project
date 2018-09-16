# Script for adding one tag to spot instances.

# Get spot instance IDs from spot requests
OUTPUT=$(aws ec2 describe-spot-instance-requests --query SpotInstanceRequests[*].{ID:InstanceId})

# Tags that will apply to all spot instances
KEY='Project'
VALUE='Phase2'

# For each spot instance, create the above tag.
echo $OUTPUT | grep -o '"ID": "[^"]*' | grep -o '[^"]*$' | while read -r ID ; do
   aws ec2 create-tags --resources $ID --tags Key='Project',Value='Phase2'
   echo "Tagged spot instance $ID with $KEY : $VALUE"
done
