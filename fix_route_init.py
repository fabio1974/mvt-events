#!/usr/bin/env python3
import os

fp = os.path.join(os.path.dirname(__file__), 'src/main/java/com/mvt/mvt_events/service/DeliveryService.java')
with open(fp, 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Find the line that contains "ROUTE TRACKING: Initialize route with courier"
target_line_idx = None
for i, line in enumerate(lines):
    if "ROUTE TRACKING: Initialize route with courier" in line:
        target_line_idx = i
        break

if target_line_idx is None:
    print("ERROR: Could not find target line")
    exit(1)

print(f"Found target at line {target_line_idx + 1}")

# Find the end of the try-catch block (line with just "        }")
end_idx = target_line_idx
for i in range(target_line_idx, min(target_line_idx + 15, len(lines))):
    if lines[i].strip() == '}' and '} catch' not in lines[i]:
        # Check if previous non-empty line has the catch closing
        end_idx = i
        # Keep going - we want the outer try's closing brace
        # The pattern is: try { ... } catch (Exception e) { ... }
        # So we need the last } of the catch block
    if lines[i].strip() == '}' and i > target_line_idx + 5:
        # This should be the closing of the catch block
        end_idx = i
        break

print(f"Block ends at line {end_idx + 1}")
print(f"Lines to replace ({target_line_idx+1} to {end_idx+1}):")
for i in range(target_line_idx, end_idx + 1):
    print(f"  {i+1}: {lines[i].rstrip()}")

new_lines = [
    '        // \U0001f4cd ROUTE TRACKING: Fallback - initialize route if not already done on ACCEPTED\n',
    '        try {\n',
    '            User courier = delivery.getCourier();\n',
    '            if (courier != null && courier.getGpsLatitude() != null && courier.getGpsLongitude() != null) {\n',
    '                // Only initialize if route was not already created on ACCEPTED\n',
    '                String existingRoute = deliveryRepository.getRouteAsGeoJson(saved.getId());\n',
    '                if (existingRoute == null || existingRoute.isEmpty()) {\n',
    '                    deliveryRepository.initializeRoute(saved.getId(), courier.getGpsLatitude(), courier.getGpsLongitude());\n',
    '                    System.out.println("\U0001f4cd Route initialized (fallback on IN_TRANSIT) for delivery " + saved.getId());\n',
    '                }\n',
    '            }\n',
    '        } catch (Exception e) {\n',
    '            System.err.println("\u26a0\ufe0f Failed to initialize route for delivery " + saved.getId() + ": " + e.getMessage());\n',
    '        }\n',
]

result = lines[:target_line_idx] + new_lines + lines[end_idx + 1:]

with open(fp, 'w', encoding='utf-8') as f:
    f.writelines(result)

print("SUCCESS: Replaced route init in confirmPickup with fallback version")
