function gsd = computeGSD(EO, R, nv, d, focalLength, pixelSize)
% Compute GSD of each image
% Output - 31.08.2017
%   gsd: Ground Sampling Distance(GSD) of each image(m/pix)
% Input
%   EO: position/attitude parameter of a camera (1x6)
%   R: rotation matrix (3x3)
%   nv: normal vector of target surface (3x1)
%   d: constant of target surface (1x1)
%   focalLength: [m]
%   pixelSize: [m/pix]

    CoI = [0 0 -focalLength]';  % in Camera Coordinate System [m]
    scale = (d - nv' * EO(1:3)') / (nv' * R' * CoI);    % none unit
    gsd = scale * pixelSize;
end

