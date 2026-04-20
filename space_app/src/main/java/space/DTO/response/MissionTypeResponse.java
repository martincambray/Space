package space.DTO.response;

import space.MODEL.MissionType;

public class MissionTypeResponse 
{

    private int id;
    private String name;
    private String description;

    public static MissionTypeResponse convert(MissionType t) 
    {
        MissionTypeResponse resp = new MissionTypeResponse();
        resp.setId(t.getId());
        resp.setName(t.getName());
        resp.setDescription(t.getDescription());
        return resp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
